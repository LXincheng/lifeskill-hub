package dev.lifeskill.agent.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import dev.lifeskill.agent.application.port.AgentModelPort;
import dev.lifeskill.agent.application.port.AgentRunEventPort;
import dev.lifeskill.agent.application.port.AgentRunRepository;
import dev.lifeskill.agent.application.port.OfficialSourcePort;
import dev.lifeskill.agent.domain.AgentPolicyGate;
import dev.lifeskill.agent.domain.AgentRun;
import dev.lifeskill.agent.domain.AgentRunStatus;
import dev.lifeskill.agent.domain.Claim;
import dev.lifeskill.agent.domain.Evidence;
import dev.lifeskill.pulse.application.PulseApplicationService;
import dev.lifeskill.shared.application.IdGenerator;
import dev.lifeskill.skill.application.SkillApplicationService;
import dev.lifeskill.skill.domain.SkillStatus;

@Service
public class AgentRunApplicationService {
    static final int MAX_STEPS = 8;
    static final Duration RUN_TIMEOUT = Duration.ofSeconds(120);

    private final AgentRunRepository repository;
    private final SkillApplicationService skills;
    private final OfficialSourcePort source;
    private final AgentModelPort model;
    private final AgentPolicyGate policyGate;
    private final PulseApplicationService pulse;
    private final AgentRunEventPort events;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final Executor executor;

    public AgentRunApplicationService(
            AgentRunRepository repository,
            SkillApplicationService skills,
            OfficialSourcePort source,
            AgentModelPort model,
            AgentPolicyGate policyGate,
            PulseApplicationService pulse,
            AgentRunEventPort events,
            IdGenerator idGenerator,
            Clock clock,
            @Qualifier("agentRunExecutor") Executor executor) {
        this.repository = repository;
        this.skills = skills;
        this.source = source;
        this.model = model;
        this.policyGate = policyGate;
        this.pulse = pulse;
        this.events = events;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.executor = executor;
    }

    @Transactional
    public AgentRunDetails startManual(UUID skillId) {
        return start(skillId, "MANUAL", null);
    }

    @Transactional
    public AgentRunDetails startScheduled(UUID skillId, String scheduleSlot) {
        return start(skillId, "SCHEDULED", scheduleSlot);
    }

    @Transactional(readOnly = true)
    public AgentRunDetails get(UUID runId) {
        AgentRun run = repository.findRun(runId).orElseThrow(() -> new AgentRunNotFoundException(runId));
        return new AgentRunDetails(run, repository.findSteps(runId));
    }

    @Transactional(readOnly = true)
    public AgentRunDetails latest(UUID skillId) {
        AgentRun run = repository.findLatestBySkill(skillId).orElseThrow(() -> new AgentRunNotFoundException(skillId));
        return new AgentRunDetails(run, repository.findSteps(run.id()));
    }

    private AgentRunDetails start(UUID skillId, String triggerType, String scheduleSlot) {
        var details = skills.get(skillId);
        if (details.skill().status() != SkillStatus.ACTIVE) {
            throw new IllegalArgumentException("Paused Skill cannot start a new AgentRun");
        }
        Instant now = clock.instant();
        UUID runId = idGenerator.nextId();
        repository.create(
                runId, skillId, details.skill().currentVersion(), idGenerator.nextId(), triggerType,
                scheduleSlot, MAX_STEPS, now, now.plus(RUN_TIMEOUT));
        recordStep(runId, "Harness", AgentRunStatus.RECEIVED, "REQUEST_ACCEPTED",
                "已确认 Skill 与版本", "运行已进入受控状态机", null, null, 0L, null);
        AgentRunDetails created = get(runId);
        dispatchAfterCommit(runId);
        return created;
    }

    private void dispatchAfterCommit(UUID runId) {
        Runnable dispatch = () -> executor.execute(() -> execute(runId));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch.run();
            }
        });
    }

    void execute(UUID runId) {
        AgentRun initial = repository.findRun(runId).orElseThrow(() -> new AgentRunNotFoundException(runId));
        long runStarted = System.nanoTime();
        try {
            var skill = skills.get(initial.skillId());
            step(runId, "Planner", AgentRunStatus.PLANNING, "PLAN_CREATED",
                    "Skill v" + initial.skillVersion(), "锁定 Java Agent Weekly 官方更新目标", null, null,
                    () -> skill.version().objective());

            List<OfficialSourcePort.OfficialSourceDocument> documents = step(
                    runId, "Researcher", AgentRunStatus.COLLECTING, "TOOL_COMPLETED",
                    "仅允许 Spring AI 官方仓库", "GitHub Release 采集完成", "spring-ai-github-releases",
                    "https://github.com/spring-projects/spring-ai/releases",
                    () -> source.collect(clock.instant()));
            if (documents.isEmpty()) throw new IllegalStateException("Official source returned no stable releases");

            List<Evidence> evidence = documents.stream().map(document -> repository.saveEvidence(new Evidence(
                    idGenerator.nextId(), runId, document.sourceType(), document.sourceName(), document.sourceUrl(),
                    document.externalId(), document.title(), document.excerpt(), document.rawContent(), document.publishedAt(),
                    document.fetchedAt(), document.contentHash(), document.officialSource()))).toList();

            AgentModelPort.ResearchResult research = step(
                    runId, "Researcher", AgentRunStatus.RESEARCHING, "CLAIM_DRAFTED",
                    evidence.size() + " 条 Evidence", "生成引用 Evidence ID 的 Claim", "deepseek-researcher", null,
                    () -> model.research(skill.version().objective(), evidence));
            List<UUID> researchIds = validatedEvidenceIds(research.evidenceIds(), evidence);
            Claim draftClaim = repository.saveClaim(new Claim(
                    idGenerator.nextId(), runId, research.statement(), researchIds, "PENDING", 0,
                    null, clock.instant(), null));

            AgentModelPort.VerificationResult verification = step(
                    runId, "Verifier", AgentRunStatus.VERIFYING, "CLAIM_VERIFIED",
                    "独立上下文重新读取 Claim 与 Evidence", "核验完成", "deepseek-verifier", null,
                    () -> model.verify(skill.version().objective(), draftClaim, evidence));
            List<UUID> verifierIds = validatedEvidenceIds(verification.evidenceIds(), evidence);
            if (!new HashSet<>(verifierIds).containsAll(draftClaim.evidenceIds())) {
                throw new IllegalStateException("Verifier did not independently confirm every cited Evidence ID");
            }
            Claim claim = repository.updateVerification(
                    draftClaim.id(), verification.verified() ? "VERIFIED" : "REJECTED",
                    verification.confidence(), verification.summary(), clock.instant());

            if (!verification.verified()) {
                block(runId, runStarted, "VERIFICATION_FAILED", verification.summary());
                return;
            }

            Claim verifiedClaim = claim;
            AgentModelPort.CompositionResult composition = step(
                    runId, "Composer", AgentRunStatus.COMPOSING, "PULSE_COMPOSED",
                    "只使用已核验 Claim", "动态草稿已生成", "deepseek-composer", null,
                    () -> model.compose(skill.version().objective(), verifiedClaim, evidence));

            var decision = policyGate.evaluate(verifiedClaim, evidence);
            recordStep(runId, "Java Policy Gate", AgentRunStatus.POLICY_CHECK,
                    decision.allowed() ? "PUBLICATION_ALLOWED" : "PUBLICATION_BLOCKED",
                    "确定性检查 Evidence、官方来源、核验状态和阈值", decision.reason(), null, null, 0L,
                    decision.allowed() ? null : decision.code());
            if (!decision.allowed()) {
                finish(runId, AgentRunStatus.BLOCKED, runStarted, decision.reason());
                return;
            }

            pulse.publishVerified(
                    runId, verifiedClaim.id(), composition.category(), composition.title(), composition.summary(),
                    verifiedClaim.evidenceIds().size(), composition.recommendationReason());
            finish(runId, AgentRunStatus.COMPLETED, runStarted, null);
        } catch (RuntimeException exception) {
            AgentRun current = repository.findRun(runId).orElse(initial);
            AgentRunStatus status = clock.instant().isAfter(current.timeoutAt())
                    ? AgentRunStatus.TIMED_OUT : AgentRunStatus.FAILED;
            String summary = safeFailure(exception);
            if (current.stepCount() < current.maxSteps()) {
                recordStep(runId, "Java Harness", status, "RUN_FAILED", "运行安全终止", null,
                        null, null, 0L, summary);
            }
            finish(runId, status, runStarted, summary);
        }
    }

    private <T> T step(
            UUID runId, String role, AgentRunStatus status, String eventType, String input, String output,
            String toolName, String sourceUrl, java.util.function.Supplier<T> action) {
        ensureWithinLimits(runId);
        long started = System.nanoTime();
        try {
            T result = action.get();
            recordStep(runId, role, status, eventType, input, output, toolName, sourceUrl, elapsed(started), null);
            return result;
        } catch (RuntimeException exception) {
            recordStep(runId, role, status, eventType + "_FAILED", input, null, toolName, sourceUrl,
                    elapsed(started), safeFailure(exception));
            throw exception;
        }
    }

    private void recordStep(
            UUID runId, String role, AgentRunStatus runStatus, String eventType, String input, String output,
            String toolName, String sourceUrl, Long durationMs, String errorSummary) {
        AgentRun run = repository.findRun(runId).orElseThrow(() -> new AgentRunNotFoundException(runId));
        int order = run.stepCount() + 1;
        if (order > run.maxSteps()) throw new IllegalStateException("AgentRun exceeded maximum steps");
        Instant now = clock.instant();
        repository.addStep(
                idGenerator.nextId(), runId, order, role, errorSummary == null ? "COMPLETED" : "FAILED", eventType,
                input, output, toolName, sourceUrl, durationMs, errorSummary, now, now, runStatus);
        events.changed(runId);
    }

    private void ensureWithinLimits(UUID runId) {
        AgentRun run = repository.findRun(runId).orElseThrow(() -> new AgentRunNotFoundException(runId));
        if (run.stepCount() >= run.maxSteps()) throw new IllegalStateException("AgentRun exceeded maximum steps");
        if (clock.instant().isAfter(run.timeoutAt())) throw new IllegalStateException("AgentRun exceeded its timeout");
    }

    private List<UUID> validatedEvidenceIds(List<String> ids, List<Evidence> evidence) {
        Set<UUID> available = evidence.stream().map(Evidence::id).collect(java.util.stream.Collectors.toSet());
        List<UUID> parsed;
        try {
            parsed = ids.stream().map(UUID::fromString).distinct().toList();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Model returned an invalid Evidence ID", exception);
        }
        if (parsed.isEmpty() || !available.containsAll(parsed)) {
            throw new IllegalStateException("Model cited Evidence outside this run");
        }
        return parsed;
    }

    private void block(UUID runId, long runStarted, String code, String reason) {
        recordStep(runId, "Java Policy Gate", AgentRunStatus.POLICY_CHECK, "PUBLICATION_BLOCKED",
                "确定性发布规则", reason, null, null, 0L, code);
        finish(runId, AgentRunStatus.BLOCKED, runStarted, reason);
    }

    private void finish(UUID runId, AgentRunStatus status, long runStarted, String failureSummary) {
        repository.finish(runId, status, clock.instant(), elapsed(runStarted), failureSummary);
        events.finished(runId);
    }

    private long elapsed(long started) {
        return Math.max(0, Duration.ofNanos(System.nanoTime() - started).toMillis());
    }

    private String safeFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.length() <= 240 ? message : message.substring(0, 240);
    }
}
