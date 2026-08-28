package dev.lifeskill.learning.api.dto;

import java.time.Instant;

import dev.lifeskill.learning.application.LearningApplicationService.LearningProgress;

public record LearningProgressResponse(
        int contentCount,
        int startedCount,
        int completedCount,
        int completionPercent,
        Double averageQuizScore,
        Instant latestActivityAt) {
    public static LearningProgressResponse from(LearningProgress progress) {
        return new LearningProgressResponse(
                progress.contentCount(), progress.startedCount(), progress.completedCount(),
                progress.completionPercent(), progress.averageQuizScore(), progress.latestActivityAt());
    }
}
