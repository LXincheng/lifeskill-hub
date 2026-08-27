package dev.lifeskill.conversation.domain;

import java.util.Objects;

public record ProcessingStep(
        String stage,
        String label,
        ProcessingStepStatus status,
        long durationMs,
        String detail) {

    public ProcessingStep {
        stage = requireText(stage, "Processing stage");
        label = requireText(label, "Processing label");
        Objects.requireNonNull(status, "Processing status is required");
        if (durationMs < 0) {
            throw new IllegalArgumentException("Processing duration must not be negative");
        }
        detail = detail == null ? "" : detail.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
