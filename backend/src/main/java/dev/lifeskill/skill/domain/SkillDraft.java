package dev.lifeskill.skill.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SkillDraft(
        UUID id,
        UUID conversationId,
        UUID sourceMessageId,
        String title,
        String objective,
        WeeklySchedule schedule,
        SkillDraftStatus status,
        String promptVersion,
        Instant createdAt,
        Instant updatedAt) {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_OBJECTIVE_LENGTH = 1_000;

    public SkillDraft {
        Objects.requireNonNull(id, "Skill draft id is required");
        Objects.requireNonNull(conversationId, "Conversation id is required");
        Objects.requireNonNull(sourceMessageId, "Source message id is required");
        title = requireText(title, "Skill draft title", MAX_TITLE_LENGTH);
        objective = requireText(objective, "Skill draft objective", MAX_OBJECTIVE_LENGTH);
        Objects.requireNonNull(schedule, "Skill draft schedule is required");
        Objects.requireNonNull(status, "Skill draft status is required");
        promptVersion = requireText(promptVersion, "Prompt version", 40);
        Objects.requireNonNull(createdAt, "Skill draft creation time is required");
        Objects.requireNonNull(updatedAt, "Skill draft update time is required");
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
