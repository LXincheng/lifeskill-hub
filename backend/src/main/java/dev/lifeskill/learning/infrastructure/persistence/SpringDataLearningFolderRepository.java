package dev.lifeskill.learning.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLearningFolderRepository extends JpaRepository<LearningFolderEntity, UUID> {
    List<LearningFolderEntity> findAllByOrderByUpdatedAtDesc();
}
