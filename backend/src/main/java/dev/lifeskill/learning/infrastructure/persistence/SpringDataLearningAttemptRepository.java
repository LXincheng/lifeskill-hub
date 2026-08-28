package dev.lifeskill.learning.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLearningAttemptRepository extends JpaRepository<LearningAttemptEntity, UUID> {
    List<LearningAttemptEntity> findAllByContentItemIdOrderByCreatedAtDesc(UUID contentItemId);
    List<LearningAttemptEntity> findAllByContentItemIdInOrderByCreatedAtDesc(List<UUID> contentItemIds);
}
