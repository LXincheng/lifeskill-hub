package dev.lifeskill.conversation.application.port;

import java.util.Optional;
import java.util.UUID;

import dev.lifeskill.conversation.domain.Conversation;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID conversationId);
}
