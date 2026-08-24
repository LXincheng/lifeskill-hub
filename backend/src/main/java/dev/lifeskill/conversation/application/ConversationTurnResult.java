package dev.lifeskill.conversation.application;

import java.util.List;

import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.skill.domain.SkillDraft;

public record ConversationTurnResult(Conversation conversation, List<SkillDraft> skillDrafts) {

    public ConversationTurnResult {
        skillDrafts = List.copyOf(skillDrafts);
    }
}
