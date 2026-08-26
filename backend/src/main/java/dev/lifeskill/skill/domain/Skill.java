package dev.lifeskill.skill.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Skill(
        UUID id,
        UUID sourceDraftId,
        String name,
        String description,
        SkillStatus status,
        int currentVersion,
        Instant createdAt,
        Instant updatedAt) {

    public Skill {
        Objects.requireNonNull(id, "Skill id is required");
        Objects.requireNonNull(sourceDraftId, "Source draft id is required");
        name = requireText(name, "Skill name", SkillDraft.MAX_TITLE_LENGTH);
        description = requireText(description, "Skill description", SkillDraft.MAX_OBJECTIVE_LENGTH);
        Objects.requireNonNull(status, "Skill status is required");
        if (currentVersion < 1) {
            throw new IllegalArgumentException("Current version must be positive");
        }
        Objects.requireNonNull(createdAt, "Skill creation time is required");
        Objects.requireNonNull(updatedAt, "Skill update time is required");
    }

    public Skill changeStatus(SkillStatus nextStatus, Instant changedAt) {
        Objects.requireNonNull(nextStatus, "Skill status is required");
        Objects.requireNonNull(changedAt, "Skill update time is required");
        if (status == nextStatus) {
            return this;
        }
        return new Skill(id, sourceDraftId, name, description, nextStatus, currentVersion, createdAt, changedAt);
    }

    public Skill revise(String nextName, String nextDescription, int nextVersion, Instant changedAt) {
        return new Skill(
                id,
                sourceDraftId,
                nextName,
                nextDescription,
                status,
                nextVersion,
                createdAt,
                Objects.requireNonNull(changedAt, "Skill update time is required"));
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }
}
