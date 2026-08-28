package dev.lifeskill.learning.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LearningAnnotation(
        UUID id, UUID contentItemId, LearningAnnotationKind kind,
        String selectedText, String note, Instant createdAt) {
    public LearningAnnotation {
        Objects.requireNonNull(id, "Learning annotation id is required");
        Objects.requireNonNull(contentItemId, "Content item id is required");
        Objects.requireNonNull(kind, "Learning annotation kind is required");
        Objects.requireNonNull(createdAt, "Learning annotation creation time is required");
        selectedText = normalize(selectedText);
        note = normalize(note);
        if (selectedText != null && selectedText.length() > 2_000) throw new IllegalArgumentException("Selected text is too long");
        if (note != null && note.length() > 2_000) throw new IllegalArgumentException("Annotation note is too long");
        if (kind == LearningAnnotationKind.HIGHLIGHT && selectedText == null) {
            throw new IllegalArgumentException("A highlight requires selected text");
        }
        if (kind == LearningAnnotationKind.FEEDBACK && note == null) {
            throw new IllegalArgumentException("Feedback requires a note");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
