package dev.lifeskill.learning.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ContentItem(
        UUID id,
        UUID folderId,
        UUID sourceSkillRunId,
        ContentItemType type,
        String title,
        String body,
        String verificationStatus,
        Instant createdAt,
        Instant updatedAt) {

    public ContentItem(UUID id, UUID folderId, ContentItemType type, String title, String body, Instant createdAt, Instant updatedAt) {
        this(id, folderId, null, type, title, body, "USER_AUTHORED", createdAt, updatedAt);
    }

    public ContentItem {
        Objects.requireNonNull(id, "Content item id is required");
        Objects.requireNonNull(folderId, "Learning folder id is required");
        Objects.requireNonNull(type, "Content item type is required");
        title = requireText(title, "Content item title", 240);
        body = requireText(body, "Content item body", 20_000);
        Objects.requireNonNull(verificationStatus, "Content verification status is required");
        Objects.requireNonNull(createdAt, "Content item creation time is required");
        Objects.requireNonNull(updatedAt, "Content item update time is required");
    }

    public ContentItem update(ContentItemType nextType, String nextTitle, String nextBody, Instant changedAt) {
        // 人工改写后，原来的官方核验只能证明旧版本；必须降级标记，避免编辑后的内容继续冒充已核验事实。
        String nextVerificationStatus = switch (verificationStatus) {
            case "VERIFIED" -> "PARTIALLY_VERIFIED";
            case "AI_GENERATED" -> "USER_AUTHORED";
            default -> verificationStatus;
        };
        return new ContentItem(
                id,
                folderId,
                sourceSkillRunId,
                nextType == null ? type : nextType,
                nextTitle == null ? title : nextTitle,
                nextBody == null ? body : nextBody,
                nextVerificationStatus,
                createdAt,
                Objects.requireNonNull(changedAt, "Content item update time is required"));
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(fieldName + " is too long");
        return normalized;
    }
}
