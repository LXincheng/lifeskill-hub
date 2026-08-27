package dev.lifeskill.conversation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Conversation {

    public static final String DEFAULT_TITLE = "新对话";
    private static final int GENERATED_TITLE_LENGTH = 32;

    private final UUID id;
    private String title;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<Message> messages;

    private Conversation(UUID id, String title, Instant createdAt, Instant updatedAt, List<Message> messages) {
        this.id = Objects.requireNonNull(id, "Conversation id is required");
        this.title = requireTitle(title);
        this.createdAt = Objects.requireNonNull(createdAt, "Conversation creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Conversation update time is required");
        this.messages = new ArrayList<>(Objects.requireNonNull(messages, "Conversation messages are required"));
    }

    public static Conversation start(UUID id, Instant now) {
        return new Conversation(id, DEFAULT_TITLE, now, now, List.of());
    }

    public static Conversation restore(
            UUID id,
            String title,
            Instant createdAt,
            Instant updatedAt,
            List<Message> messages) {
        return new Conversation(id, title, createdAt, updatedAt, messages);
    }

    public Message addUserMessage(UUID messageId, String content, Instant now) {
        Message message = new Message(messageId, MessageRole.USER, content, now);
        if (messages.isEmpty()) {
            title = generateTitle(message.content());
        }
        messages.add(message);
        updatedAt = now;
        return message;
    }

    public Message addAssistantMessage(UUID messageId, String content, Instant now) {
        return addAssistantMessage(messageId, content, now, List.of(), null);
    }

    public Message addAssistantMessage(
            UUID messageId,
            String content,
            Instant now,
            List<ProcessingStep> processingSteps,
            Long durationMs) {
        Message message = new Message(
                messageId, MessageRole.ASSISTANT, content, now, processingSteps, durationMs);
        messages.add(message);
        updatedAt = now;
        return message;
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Conversation title must not be blank");
        }
        return title.trim();
    }

    private static String generateTitle(String content) {
        String singleLine = content.replaceAll("\\s+", " ");
        int codePointCount = singleLine.codePointCount(0, singleLine.length());
        if (codePointCount <= GENERATED_TITLE_LENGTH) {
            return singleLine;
        }
        int endIndex = singleLine.offsetByCodePoints(0, GENERATED_TITLE_LENGTH);
        return singleLine.substring(0, endIndex) + "…";
    }
}
