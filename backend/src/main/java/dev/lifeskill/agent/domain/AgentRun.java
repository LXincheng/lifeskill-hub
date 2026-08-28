package dev.lifeskill.agent.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentRun(
        UUID id,
        UUID skillId,
        int skillVersion,
        UUID conversationId,
        UUID sourceMessageId,
        String objective,
        String capability,
        UUID resultContentId,
        UUID auditId,
        String triggerType,
        AgentRunStatus status,
        int maxSteps,
        int stepCount,
        Instant startedAt,
        Instant timeoutAt,
        Instant completedAt,
        Long durationMs,
        String failureSummary,
        Instant createdAt) {

    public AgentRun {
        Objects.requireNonNull(id, "Run id is required");
        Objects.requireNonNull(auditId, "Audit id is required");
        Objects.requireNonNull(triggerType, "Trigger type is required");
        Objects.requireNonNull(status, "Run status is required");
        Objects.requireNonNull(startedAt, "Run start time is required");
        Objects.requireNonNull(timeoutAt, "Run timeout is required");
        Objects.requireNonNull(createdAt, "Run creation time is required");
        boolean skillRun = skillId != null && skillVersion >= 1;
        boolean researchRun = skillId == null && skillVersion == 0 && conversationId != null
                && sourceMessageId != null && objective != null && !objective.isBlank()
                && capability != null && !capability.isBlank();
        if ((!skillRun && !researchRun) || maxSteps < 1 || stepCount < 0 || stepCount > maxSteps) {
            throw new IllegalArgumentException("Agent run limits are invalid");
        }
    }
}
