package dev.lifeskill.skill.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.shared.application.IdGenerator;
import dev.lifeskill.skill.application.port.SkillDraftRepository;
import dev.lifeskill.skill.application.port.SkillRepository;
import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillDraft;
import dev.lifeskill.skill.domain.SkillStatus;
import dev.lifeskill.skill.domain.SkillVersion;

@Service
public class SkillConfirmationApplicationService {

    private final SkillDraftRepository draftRepository;
    private final SkillRepository skillRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SkillConfirmationApplicationService(
            SkillDraftRepository draftRepository,
            SkillRepository skillRepository,
            Clock clock,
            IdGenerator idGenerator) {
        this.draftRepository = draftRepository;
        this.skillRepository = skillRepository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public SkillConfirmationResult confirm(UUID draftId, String idempotencyKey) {
        String normalizedKey = normalizeKey(idempotencyKey);
        draftRepository.findByConfirmationKey(normalizedKey)
                .filter(existing -> !existing.id().equals(draftId))
                .ifPresent(existing -> {
                    throw new IdempotencyConflictException();
                });

        SkillDraft draft = draftRepository.findByIdForUpdate(draftId)
                .orElseThrow(() -> new SkillDraftNotFoundException(draftId));
        if (draft.confirmedSkillId() != null) {
            Skill skill = skillRepository.findById(draft.confirmedSkillId())
                    .orElseThrow(() -> new IllegalStateException("Confirmed skill is missing"));
            SkillVersion version = skillRepository.findVersion(skill.id(), skill.currentVersion())
                    .orElseThrow(() -> new IllegalStateException("Confirmed skill version is missing"));
            return new SkillConfirmationResult(
                    draft,
                    skill,
                    version,
                    false);
        }

        var now = clock.instant();
        UUID skillId = idGenerator.nextId();
        Skill skill = new Skill(
                skillId,
                draft.id(),
                draft.title(),
                draft.objective(),
                SkillStatus.ACTIVE,
                1,
                now,
                now);
        SkillVersion version = new SkillVersion(
                idGenerator.nextId(),
                skillId,
                1,
                draft.objective(),
                draft.schedule(),
                now);
        skillRepository.save(skill, version);
        SkillDraft confirmedDraft = draft.confirm(skillId, normalizedKey, now);
        draftRepository.save(confirmedDraft);
        return new SkillConfirmationResult(confirmedDraft, skill, version, true);
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }
        String normalized = value.trim();
        if (normalized.length() > 120) {
            throw new InvalidIdempotencyKeyException();
        }
        return normalized;
    }
}
