package dev.lifeskill.learning.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LearningFolder(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public LearningFolder {
        Objects.requireNonNull(id, "Learning folder id is required");
        name = requireText(name, "Learning folder name", 160);
        description = normalize(description, 1000);
        Objects.requireNonNull(createdAt, "Learning folder creation time is required");
        Objects.requireNonNull(updatedAt, "Learning folder update time is required");
    }

    public LearningFolder update(String nextName, String nextDescription, Instant changedAt) {
        return new LearningFolder(
                id,
                nextName == null ? name : nextName,
                nextDescription == null ? description : nextDescription,
                createdAt,
                Objects.requireNonNull(changedAt, "Learning folder update time is required"));
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(fieldName + " is too long");
        return normalized;
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("Learning folder description is too long");
        return normalized;
    }
}
