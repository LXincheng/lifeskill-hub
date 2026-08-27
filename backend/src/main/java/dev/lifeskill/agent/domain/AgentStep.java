package dev.lifeskill.agent.domain;

import java.time.Instant;
import java.util.UUID;

public record AgentStep(
        UUID id,
        UUID runId,
        int order,
        String role,
        String status,
        String eventType,
        String inputSummary,
        String outputSummary,
        String toolName,
        String sourceUrl,
        Long durationMs,
        String errorSummary,
        Instant createdAt,
        Instant completedAt) {
}
