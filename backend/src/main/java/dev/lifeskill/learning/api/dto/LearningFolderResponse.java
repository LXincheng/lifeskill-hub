package dev.lifeskill.learning.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.learning.domain.LearningFolder;

public record LearningFolderResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {
    public static LearningFolderResponse from(LearningFolder folder) {
        return new LearningFolderResponse(
                folder.id(), folder.name(), folder.description(), folder.createdAt(), folder.updatedAt());
    }
}
