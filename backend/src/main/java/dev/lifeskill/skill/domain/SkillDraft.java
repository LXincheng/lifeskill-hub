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
        UUID confirmedSkillId,
        String confirmationKey,
        Instant confirmedAt,
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
        validateConfirmation(status, confirmedSkillId, confirmationKey, confirmedAt);
        Objects.requireNonNull(createdAt, "Skill draft creation time is required");
        Objects.requireNonNull(updatedAt, "Skill draft update time is required");
    }

    public SkillDraft confirm(UUID skillId, String idempotencyKey, Instant confirmedAt) {
        Objects.requireNonNull(skillId, "Confirmed skill id is required");
        String normalizedKey = requireText(idempotencyKey, "Idempotency key", 120);
        Objects.requireNonNull(confirmedAt, "Confirmation time is required");
        if (status == SkillDraftStatus.CONFIRMED) {
            return this;
        }
        return new SkillDraft(
                id,
                conversationId,
                sourceMessageId,
                title,
                objective,
                schedule,
                SkillDraftStatus.CONFIRMED,
                promptVersion,
                skillId,
                normalizedKey,
                confirmedAt,
                createdAt,
                confirmedAt);
    }

    private static void validateConfirmation(
            SkillDraftStatus status,
            UUID confirmedSkillId,
            String confirmationKey,
            Instant confirmedAt) {
        boolean hasConfirmation = confirmedSkillId != null || confirmationKey != null || confirmedAt != null;
        if (status == SkillDraftStatus.PENDING_CONFIRMATION && hasConfirmation) {
            throw new IllegalArgumentException("Pending skill draft must not contain confirmation data");
        }
        if (status == SkillDraftStatus.CONFIRMED) {
            Objects.requireNonNull(confirmedSkillId, "Confirmed skill id is required");
            requireText(confirmationKey, "Idempotency key", 120);
            Objects.requireNonNull(confirmedAt, "Confirmation time is required");
        }
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
