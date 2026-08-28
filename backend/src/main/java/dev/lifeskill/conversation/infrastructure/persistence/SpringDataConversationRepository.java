package dev.lifeskill.conversation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    java.util.List<ConversationEntity> findAllByOrderByUpdatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "messages")
    java.util.Optional<ConversationEntity> findById(UUID id);
}
