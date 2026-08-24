package dev.lifeskill.skill.application.port;

import java.util.List;
import java.util.UUID;

import dev.lifeskill.skill.domain.SkillDraft;

public interface SkillDraftRepository {

    SkillDraft save(SkillDraft draft);

    List<SkillDraft> findByConversationId(UUID conversationId);
}
