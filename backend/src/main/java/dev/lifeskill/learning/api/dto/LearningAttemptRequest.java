package dev.lifeskill.learning.api.dto;

import java.util.List;

import dev.lifeskill.learning.domain.LearningAttemptKind;
import dev.lifeskill.learning.domain.LearningAttemptStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LearningAttemptRequest(
        @NotNull LearningAttemptKind kind,
        @NotNull LearningAttemptStatus status,
        @Min(0) int completedUnits,
        @Min(1) int totalUnits,
        List<Integer> completedUnitIndexes) {
}
