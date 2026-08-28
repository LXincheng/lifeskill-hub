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
import dev.lifeskill.agent.domain.ResearchCapability;
import dev.lifeskill.learning.application.LearningApplicationService;
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
    private final List<OfficialSourcePort> sources;
    private final AgentModelPort model;
    private final AgentPolicyGate policyGate;
    private final PulseApplicationService pulse;
    private final LearningApplicationService learning;
    private final AgentRunEventPort events;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final Executor executor;

    public AgentRunApplicationService(
            AgentRunRepository repository,
            SkillApplicationService skills,
            List<OfficialSourcePort> sources,
            AgentModelPort model,
            AgentPolicyGate policyGate,
            PulseApplicationService pulse,
            LearningApplicationService learning,
            AgentRunEventPort events,
            IdGenerator idGenerator,
            Clock clock,
            @Qualifier("agentRunExecutor") Executor executor) {
        this.repository = repository;
        this.skills = skills;
        this.sources = List.copyOf(sources);
        this.model = model;
        this.policyGate = policyGate;
        this.pulse = pulse;
        this.learning = learning;
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

    @Transactional
    public AgentRunDetails startResearch(
            UUID conversationId, UUID sourceMessageId, String objective, ResearchCapability capability) {
        if (!capability.isRunnableResearch()) {
            throw new IllegalArgumentException("This research capability has no registered official source adapter");
        }
        Instant now = clock.instant();
        UUID runId = idGenerator.nextId();
        repository.createResearch(
                runId, conversationId, sourceMessageId, objective, capability.name(), idGenerator.nextId(),
                MAX_STEPS, now, now.plus(RUN_TIMEOUT));
        recordStep(runId, "Harness", AgentRunStatus.RECEIVED, "REQUEST_ACCEPTED",
                "一次性研究请求已保存", "运行已进入受控状态机", null, null, 0L, null);
        AgentRunDetails created = get(runId);
        dispatchAfterCommit(runId);
        return created;
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
            boolean researchRun = initial.skillId() == null;
            String objective = researchRun ? initial.objective() : skills.get(initial.skillId()).version().objective();
            ResearchCapability capability = researchRun
                    ? ResearchCapability.valueOf(initial.capability()) : ResearchCapability.detect(objective);
            if (!capability.isRunnableResearch()) {
                throw new IllegalStateException("Skill objective has no registered source adapter");
            }
            step(runId, "Planner", AgentRunStatus.PLANNING, "PLAN_CREATED",
                    researchRun ? "一次性研究目标" : "Skill v" + initial.skillVersion(),
                    "已锁定研究目标与允许来源", null, null, () -> objective);

            OfficialSourcePort source = sourceFor(capability);
            String sourceLabel = capability == ResearchCapability.GOLD_MARKET
                    ? "World Gold Council 官方研究" : "Spring AI 官方仓库";
            String sourceUrl = capability == ResearchCapability.GOLD_MARKET
                    ? "https://www.gold.org/goldhub" : "https://github.com/spring-projects/spring-ai/releases";
            String toolName = capability == ResearchCapability.GOLD_MARKET
                    ? "world-gold-council-research" : "spring-ai-github-releases";

            List<OfficialSourcePort.OfficialSourceDocument> documents = step(
                    runId, "Researcher", AgentRunStatus.COLLECTING, "TOOL_COMPLETED",
                    "仅允许" + sourceLabel, sourceLabel + "采集完成", toolName, sourceUrl,
                    () -> source.collect(clock.instant()));
            if (documents.isEmpty()) throw new IllegalStateException("Official source returned no research documents");

            List<Evidence> evidence = documents.stream().map(document -> repository.saveEvidence(new Evidence(
                    idGenerator.nextId(), runId, document.sourceType(), document.sourceName(), document.sourceUrl(),
                    document.externalId(), document.title(), document.excerpt(), document.rawContent(), document.publishedAt(),
                    document.fetchedAt(), document.contentHash(), document.officialSource()))).toList();

            AgentModelPort.ResearchResult research = step(
                    runId, "Researcher", AgentRunStatus.RESEARCHING, "CLAIM_DRAFTED",
                    evidence.size() + " 条 Evidence", "生成引用 Evidence ID 的 Claim", "deepseek-researcher", null,
                    () -> model.research(objective, evidence));
            List<UUID> researchIds = validatedEvidenceIds(research.evidenceIds(), evidence);
            Claim draftClaim = repository.saveClaim(new Claim(
                    idGenerator.nextId(), runId, research.statement(), researchIds, "PENDING", 0,
                    null, clock.instant(), null));

            AgentModelPort.VerificationResult verification = step(
                    runId, "Verifier", AgentRunStatus.VERIFYING, "CLAIM_VERIFIED",
                    "独立上下文重新读取 Claim 与 Evidence", "核验完成", "deepseek-verifier", null,
                    () -> model.verify(objective, draftClaim, evidence));
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
            List<Evidence> citedEvidence = evidence.stream()
                    .filter(item -> verifiedClaim.evidenceIds().contains(item.id()))
                    .toList();
            AgentModelPort.CompositionResult composition = null;
            AgentModelPort.ReportResult report = null;
            if (researchRun) {
                report = step(runId, "Composer", AgentRunStatus.COMPOSING, "REPORT_COMPOSED",
                        "只使用已核验 Claim 与 Evidence", "专业研究报告草稿已生成", "deepseek-report-composer", null,
                        () -> model.composeReport(objective, verifiedClaim, citedEvidence));
            } else {
                composition = step(runId, "Composer", AgentRunStatus.COMPOSING, "PULSE_COMPOSED",
                        "只使用已核验 Claim", "动态草稿已生成", "deepseek-composer", null,
                        () -> model.compose(objective, verifiedClaim, citedEvidence));
            }

            var decision = policyGate.evaluate(verifiedClaim, evidence);
            recordStep(runId, "Java Policy Gate", AgentRunStatus.POLICY_CHECK,
                    decision.allowed() ? "PUBLICATION_ALLOWED" : "PUBLICATION_BLOCKED",
                    "确定性检查 Evidence、官方来源、核验状态和阈值", decision.reason(), null, null, 0L,
                    decision.allowed() ? null : decision.code());
            if (!decision.allowed()) {
                finish(runId, AgentRunStatus.BLOCKED, runStarted, decision.reason());
                return;
            }

            if (researchRun) {
                var content = learning.createVerifiedReport(
                        runId, "黄金研究", "由 World Gold Council 官方 Evidence 生成的一次性研究报告。",
                        report.title(), report.body());
                repository.attachResultContent(runId, content.id());
            } else {
                pulse.publishVerified(
                        runId, verifiedClaim.id(), composition.category(), composition.title(), composition.summary(),
                        verifiedClaim.evidenceIds().size(), composition.recommendationReason());
            }
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

    private OfficialSourcePort sourceFor(ResearchCapability capability) {
        return sources.stream().filter(source -> source.capability().equals(capability.name())).findFirst()
                .orElseThrow(() -> new IllegalStateException("No official source adapter for " + capability));
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
        if (message.contains("could not execute statement") || message.contains("Failing row contains")) {
            return "数据库写入未通过完整性校验，运行已安全终止。";
        }
        return message.length() <= 240 ? message : message.substring(0, 240);
    }
}
