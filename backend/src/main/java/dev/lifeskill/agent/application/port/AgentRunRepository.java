package dev.lifeskill.agent.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.lifeskill.agent.domain.AgentRun;
import dev.lifeskill.agent.domain.AgentRunStatus;
import dev.lifeskill.agent.domain.AgentStep;
import dev.lifeskill.agent.domain.Claim;
import dev.lifeskill.agent.domain.Evidence;

public interface AgentRunRepository {
    AgentRun create(UUID runId, UUID skillId, int version, UUID auditId, String triggerType,
                    String scheduleSlot, int maxSteps, Instant startedAt, Instant timeoutAt);

    AgentRun createResearch(UUID runId, UUID conversationId, UUID sourceMessageId, String objective,
                            String capability, UUID auditId, int maxSteps, Instant startedAt, Instant timeoutAt);

    Optional<AgentRun> findRun(UUID runId);

    Optional<AgentRun> findLatestBySkill(UUID skillId);

    List<AgentStep> findSteps(UUID runId);

    AgentStep addStep(UUID stepId, UUID runId, int order, String role, String status, String eventType,
                      String inputSummary, String outputSummary, String toolName, String sourceUrl,
                      Long durationMs, String errorSummary, Instant createdAt, Instant completedAt,
                      AgentRunStatus runStatus);

    Evidence saveEvidence(Evidence evidence);

    Claim saveClaim(Claim claim);

    Claim updateVerification(UUID claimId, String status, double confidence, String summary, Instant verifiedAt);

    void finish(UUID runId, AgentRunStatus status, Instant completedAt, long durationMs, String failureSummary);

    void attachResultContent(UUID runId, UUID contentId);

    List<Evidence> findEvidenceForClaim(UUID claimId);

    Optional<Claim> findClaim(UUID claimId);
}
