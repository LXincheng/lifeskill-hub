package dev.lifeskill.skill.infrastructure.persistence;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.lifeskill.skill.application.port.SkillDraftRepository;
import dev.lifeskill.skill.domain.SkillDraft;
import dev.lifeskill.skill.domain.WeeklySchedule;

@Repository
class JpaSkillDraftRepository implements SkillDraftRepository {

    private final SpringDataSkillDraftRepository repository;

    JpaSkillDraftRepository(SpringDataSkillDraftRepository repository) {
        this.repository = repository;
    }

    @Override
    public SkillDraft save(SkillDraft draft) {
        return toDomain(repository.save(toEntity(draft)));
    }

    @Override
    public List<SkillDraft> findByConversationId(UUID conversationId) {
        return repository.findAllByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SkillDraft> findByIdForUpdate(UUID draftId) {
        return repository.findByIdForUpdate(draftId).map(this::toDomain);
    }

    @Override
    public Optional<SkillDraft> findByConfirmationKey(String confirmationKey) {
        return repository.findByConfirmationKey(confirmationKey).map(this::toDomain);
    }

    private SkillDraftEntity toEntity(SkillDraft draft) {
        return new SkillDraftEntity(
                draft.id(),
                draft.conversationId(),
                draft.sourceMessageId(),
                draft.title(),
                draft.objective(),
                draft.schedule().dayOfWeek(),
                draft.schedule().time(),
                draft.schedule().timezone().getId(),
                draft.status(),
                draft.promptVersion(),
                draft.confirmedSkillId(),
                draft.confirmationKey(),
                draft.confirmedAt(),
                draft.createdAt(),
                draft.updatedAt());
    }

    private SkillDraft toDomain(SkillDraftEntity entity) {
        return new SkillDraft(
                entity.id(),
                entity.conversationId(),
                entity.sourceMessageId(),
                entity.title(),
                entity.objective(),
                new WeeklySchedule(
                        entity.scheduleDayOfWeek(),
                        entity.scheduleTime(),
                        ZoneId.of(entity.timezone())),
                entity.status(),
                entity.promptVersion(),
                entity.confirmedSkillId(),
                entity.confirmationKey(),
                entity.confirmedAt(),
                entity.createdAt(),
                entity.updatedAt());
    }
}
