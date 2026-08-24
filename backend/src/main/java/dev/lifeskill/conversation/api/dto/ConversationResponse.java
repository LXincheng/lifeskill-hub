package dev.lifeskill.conversation.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.conversation.domain.Message;
import dev.lifeskill.conversation.domain.MessageRole;

public record ConversationResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.id(),
                conversation.title(),
                conversation.createdAt(),
                conversation.updatedAt(),
                conversation.messages().stream().map(MessageResponse::from).toList());
    }

    public record MessageResponse(UUID id, MessageRole role, String content, Instant createdAt) {

        private static MessageResponse from(Message message) {
            return new MessageResponse(message.id(), message.role(), message.content(), message.createdAt());
        }
    }
}
