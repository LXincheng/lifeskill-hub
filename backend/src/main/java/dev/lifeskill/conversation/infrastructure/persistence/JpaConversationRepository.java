package dev.lifeskill.conversation.infrastructure.persistence;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.lifeskill.conversation.application.port.ConversationRepository;
import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.conversation.domain.Message;

@Repository
class JpaConversationRepository implements ConversationRepository {

    private final SpringDataConversationRepository repository;

    JpaConversationRepository(SpringDataConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = repository.findById(conversation.id())
                .orElseGet(() -> new ConversationEntity(
                        conversation.id(),
                        conversation.title(),
                        conversation.createdAt(),
                        conversation.updatedAt()));

        entity.update(conversation.title(), conversation.updatedAt());
        Set<UUID> persistedMessageIds = new HashSet<>(entity.messages().stream().map(MessageEntity::id).toList());
        conversation.messages().stream()
                .filter(message -> !persistedMessageIds.contains(message.id()))
                .map(this::toEntity)
                .forEach(entity::addMessage);

        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Conversation> findById(UUID conversationId) {
        return repository.findById(conversationId).map(this::toDomain);
    }

    private MessageEntity toEntity(Message message) {
        return new MessageEntity(
                message.id(),
                message.role(),
                message.content(),
                message.createdAt(),
                message.processingSteps(),
                message.durationMs(),
                message.agentRunId());
    }

    private Conversation toDomain(ConversationEntity entity) {
        return Conversation.restore(
                entity.id(),
                entity.title(),
                entity.createdAt(),
                entity.updatedAt(),
                entity.messages().stream()
                        .map(message -> new Message(
                                message.id(),
                                message.role(),
                                message.content(),
                                message.createdAt(),
                                message.processingSteps(),
                                message.durationMs(),
                                message.agentRunId()))
                        .toList());
    }
}
