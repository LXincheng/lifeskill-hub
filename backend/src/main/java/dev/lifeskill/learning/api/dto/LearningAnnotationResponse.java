package dev.lifeskill.learning.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.learning.domain.LearningAnnotation;

public record LearningAnnotationResponse(
        UUID id, UUID contentItemId, String kind, String selectedText, String note, Instant createdAt) {
    public static LearningAnnotationResponse from(LearningAnnotation annotation) {
        return new LearningAnnotationResponse(
                annotation.id(), annotation.contentItemId(), annotation.kind().name(),
                annotation.selectedText(), annotation.note(), annotation.createdAt());
    }
}
