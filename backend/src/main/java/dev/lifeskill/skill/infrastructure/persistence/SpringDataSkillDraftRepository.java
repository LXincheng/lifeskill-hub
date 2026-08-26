package dev.lifeskill.skill.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataSkillDraftRepository extends JpaRepository<SkillDraftEntity, UUID> {

    List<SkillDraftEntity> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select draft from SkillDraftEntity draft where draft.id = :draftId")
    Optional<SkillDraftEntity> findByIdForUpdate(@Param("draftId") UUID draftId);

    Optional<SkillDraftEntity> findByConfirmationKey(String confirmationKey);
}
