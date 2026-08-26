package dev.lifeskill.learning.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ContentItem(
        UUID id,
        UUID folderId,
        ContentItemType type,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt) {

    public ContentItem {
        Objects.requireNonNull(id, "Content item id is required");
        Objects.requireNonNull(folderId, "Learning folder id is required");
        Objects.requireNonNull(type, "Content item type is required");
        title = requireText(title, "Content item title", 240);
        body = requireText(body, "Content item body", 20_000);
        Objects.requireNonNull(createdAt, "Content item creation time is required");
        Objects.requireNonNull(updatedAt, "Content item update time is required");
    }

    public ContentItem update(ContentItemType nextType, String nextTitle, String nextBody, Instant changedAt) {
        return new ContentItem(
                id,
                folderId,
                nextType == null ? type : nextType,
                nextTitle == null ? title : nextTitle,
                nextBody == null ? body : nextBody,
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
