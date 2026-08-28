package dev.lifeskill.learning.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLearningAnnotationRepository extends JpaRepository<LearningAnnotationEntity, UUID> {
    List<LearningAnnotationEntity> findAllByContentItemIdOrderByCreatedAtDesc(UUID contentItemId);
}
