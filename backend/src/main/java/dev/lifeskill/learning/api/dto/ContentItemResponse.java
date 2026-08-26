package dev.lifeskill.learning.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.learning.domain.ContentItem;

public record ContentItemResponse(
        UUID id,
        UUID folderId,
        String type,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt) {
    public static ContentItemResponse from(ContentItem item) {
        return new ContentItemResponse(
                item.id(), item.folderId(), item.type().name(), item.title(), item.body(),
                item.createdAt(), item.updatedAt());
    }
}
