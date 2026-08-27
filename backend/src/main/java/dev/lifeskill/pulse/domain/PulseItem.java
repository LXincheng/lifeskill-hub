package dev.lifeskill.pulse.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PulseItem(
        UUID id,
        UUID skillRunId,
        UUID primaryClaimId,
        String category,
        String title,
        String summary,
        String verificationStatus,
        int sourceCount,
        String recommendationReason,
        Instant publishedAt,
        Instant readAt) {
    public PulseItem {
        Objects.requireNonNull(id, "Pulse item id is required");
        Objects.requireNonNull(skillRunId, "Pulse skill run id is required");
        Objects.requireNonNull(primaryClaimId, "Pulse claim id is required");
        Objects.requireNonNull(category, "Pulse category is required");
        Objects.requireNonNull(title, "Pulse title is required");
        Objects.requireNonNull(summary, "Pulse summary is required");
        Objects.requireNonNull(verificationStatus, "Pulse verification status is required");
        if (sourceCount < 1) throw new IllegalArgumentException("Verified pulse needs at least one source");
        Objects.requireNonNull(recommendationReason, "Recommendation reason is required");
        Objects.requireNonNull(publishedAt, "Pulse publish time is required");
    }
}
