package dev.lifeskill.agent.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Evidence(
        UUID id,
        UUID runId,
        String sourceType,
        String sourceName,
        String sourceUrl,
        String externalId,
        String title,
        String excerpt,
        String rawContent,
        Instant publishedAt,
        Instant fetchedAt,
        String contentHash,
        boolean officialSource) {

    public Evidence {
        Objects.requireNonNull(id, "Evidence id is required");
        Objects.requireNonNull(runId, "Run id is required");
        requireText(sourceType, "Source type");
        requireText(sourceName, "Source name");
        requireText(sourceUrl, "Source URL");
        requireText(title, "Evidence title");
        requireText(contentHash, "Content hash");
        Objects.requireNonNull(fetchedAt, "Evidence fetch time is required");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
