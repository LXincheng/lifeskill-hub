package dev.lifeskill.learning.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.learning.domain.ContentItem;

public record ContentItemResponse(
        UUID id,
        UUID folderId,
        UUID sourceSkillRunId,
        String type,
        String title,
        String body,
        String verificationStatus,
        Instant createdAt,
        Instant updatedAt) {
    public static ContentItemResponse from(ContentItem item) {
        return new ContentItemResponse(
                item.id(), item.folderId(), item.sourceSkillRunId(), item.type().name(), item.title(), item.body(),
                item.verificationStatus(),
                item.createdAt(), item.updatedAt());
    }
}
