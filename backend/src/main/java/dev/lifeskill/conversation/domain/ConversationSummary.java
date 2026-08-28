package dev.lifeskill.conversation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConversationSummary(UUID id, String title, int messageCount, Instant createdAt, Instant updatedAt) {
    public ConversationSummary {
        Objects.requireNonNull(id, "Conversation id is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Conversation title is required");
        if (messageCount < 0) throw new IllegalArgumentException("Message count cannot be negative");
        Objects.requireNonNull(createdAt, "Conversation creation time is required");
        Objects.requireNonNull(updatedAt, "Conversation update time is required");
    }
}
