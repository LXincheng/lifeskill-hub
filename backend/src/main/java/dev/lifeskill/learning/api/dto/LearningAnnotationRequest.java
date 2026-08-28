package dev.lifeskill.learning.api.dto;

import dev.lifeskill.learning.domain.LearningAnnotationKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LearningAnnotationRequest(
        @NotNull LearningAnnotationKind kind,
        @Size(max = 2000) String selectedText,
        @Size(max = 2000) String note) {
}
