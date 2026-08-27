package dev.lifeskill.agent.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Claim(
        UUID id,
        UUID runId,
        String statement,
        List<UUID> evidenceIds,
        String verificationStatus,
        double confidence,
        String verificationSummary,
        Instant createdAt,
        Instant verifiedAt) {

    public Claim {
        Objects.requireNonNull(id, "Claim id is required");
        Objects.requireNonNull(runId, "Run id is required");
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("Claim statement is required");
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        Objects.requireNonNull(verificationStatus, "Verification status is required");
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("Confidence must be between 0 and 1");
        Objects.requireNonNull(createdAt, "Claim creation time is required");
    }
}
