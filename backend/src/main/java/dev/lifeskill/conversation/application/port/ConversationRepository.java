package dev.lifeskill.conversation.application.port;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.conversation.domain.ConversationSummary;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID conversationId);

    List<ConversationSummary> findSummaries();

    void deleteById(UUID conversationId);
}
