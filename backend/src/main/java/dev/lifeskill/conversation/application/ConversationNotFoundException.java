package dev.lifeskill.conversation.application;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation %s was not found".formatted(conversationId));
    }
}
