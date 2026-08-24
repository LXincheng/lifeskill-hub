package dev.lifeskill.skill.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.skill.application.port.SkillDraftRepository;
import dev.lifeskill.skill.domain.SkillDraft;

@Service
public class SkillDraftApplicationService {

    private final SkillDraftRepository repository;

    public SkillDraftApplicationService(SkillDraftRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SkillDraft> findByConversationId(UUID conversationId) {
        return repository.findByConversationId(conversationId);
    }
}
