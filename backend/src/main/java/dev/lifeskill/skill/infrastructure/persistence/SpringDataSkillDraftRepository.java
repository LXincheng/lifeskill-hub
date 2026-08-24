package dev.lifeskill.skill.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSkillDraftRepository extends JpaRepository<SkillDraftEntity, UUID> {

    List<SkillDraftEntity> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
