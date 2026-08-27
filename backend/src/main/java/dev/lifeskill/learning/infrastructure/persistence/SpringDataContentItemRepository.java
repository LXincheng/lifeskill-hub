package dev.lifeskill.learning.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataContentItemRepository extends JpaRepository<ContentItemEntity, UUID> {
    List<ContentItemEntity> findAllByFolderIdOrderByUpdatedAtDesc(UUID folderId);
    List<ContentItemEntity> findAllBySourceSkillRunIdOrderByUpdatedAtAsc(UUID sourceSkillRunId);
    void deleteAllByFolderId(UUID folderId);
}
