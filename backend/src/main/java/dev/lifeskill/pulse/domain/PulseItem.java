package dev.lifeskill.pulse.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PulseItem(
        UUID id,
        String category,
        String title,
        String summary,
        String verificationStatus,
        Instant publishedAt,
        Instant readAt) {
    public PulseItem {
        Objects.requireNonNull(id, "Pulse item id is required");
        Objects.requireNonNull(category, "Pulse category is required");
        Objects.requireNonNull(title, "Pulse title is required");
        Objects.requireNonNull(summary, "Pulse summary is required");
        Objects.requireNonNull(verificationStatus, "Pulse verification status is required");
        Objects.requireNonNull(publishedAt, "Pulse publish time is required");
    }
}
