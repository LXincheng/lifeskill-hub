package dev.lifeskill.learning.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.lifeskill.learning.domain.LearningAttempt;

public record LearningAttemptResponse(
        UUID id,
        UUID contentItemId,
        String kind,
        String status,
        int completedUnits,
        int totalUnits,
        Double score,
        List<Integer> completedUnitIndexes,
        Instant completedAt,
        Instant createdAt) {
    public static LearningAttemptResponse from(LearningAttempt attempt) {
        return new LearningAttemptResponse(
                attempt.id(), attempt.contentItemId(), attempt.kind().name(), attempt.status().name(),
                attempt.completedUnits(), attempt.totalUnits(), attempt.score(), attempt.completedUnitIndexes(),
                attempt.completedAt(), attempt.createdAt());
    }
}
