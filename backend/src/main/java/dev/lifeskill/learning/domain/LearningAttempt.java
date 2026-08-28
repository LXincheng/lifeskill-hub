package dev.lifeskill.learning.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LearningAttempt(
        UUID id,
        UUID contentItemId,
        LearningAttemptKind kind,
        LearningAttemptStatus status,
        int completedUnits,
        int totalUnits,
        Double score,
        List<Integer> completedUnitIndexes,
        Instant completedAt,
        Instant createdAt) {

    public LearningAttempt {
        Objects.requireNonNull(id, "Learning attempt id is required");
        Objects.requireNonNull(contentItemId, "Content item id is required");
        Objects.requireNonNull(kind, "Learning attempt kind is required");
        Objects.requireNonNull(status, "Learning attempt status is required");
        Objects.requireNonNull(createdAt, "Learning attempt creation time is required");
        completedUnitIndexes = completedUnitIndexes == null ? List.of() : completedUnitIndexes.stream().distinct().sorted().toList();
        if (totalUnits < 1 || completedUnits < 0 || completedUnits > totalUnits) {
            throw new IllegalArgumentException("Learning attempt units are invalid");
        }
        if (completedUnitIndexes.stream().anyMatch(index -> index < 0 || index >= totalUnits)) {
            throw new IllegalArgumentException("Completed unit index is outside the learning content");
        }
        if (kind == LearningAttemptKind.PROGRESS && completedUnitIndexes.size() != completedUnits) {
            throw new IllegalArgumentException("Progress count must match completed unit indexes");
        }
        if (kind == LearningAttemptKind.QUIZ && (score == null || score < 0 || score > 100)) {
            throw new IllegalArgumentException("Quiz score must be between 0 and 100");
        }
        if (kind == LearningAttemptKind.PROGRESS && score != null) {
            throw new IllegalArgumentException("Progress attempts do not carry a score");
        }
        if ((status == LearningAttemptStatus.COMPLETED) != (completedAt != null)) {
            throw new IllegalArgumentException("Completed attempts require a completion time");
        }
    }
}
