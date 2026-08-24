package dev.lifeskill.conversation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Message(UUID id, MessageRole role, String content, Instant createdAt) {

    public static final int MAX_CONTENT_LENGTH = 4_000;

    public Message {
        Objects.requireNonNull(id, "Message id is required");
        Objects.requireNonNull(role, "Message role is required");
        Objects.requireNonNull(createdAt, "Message creation time is required");
        content = normalizeContent(content);
    }

    private static String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }

        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Message content must not exceed 4000 characters");
        }
        return normalized;
    }
}
