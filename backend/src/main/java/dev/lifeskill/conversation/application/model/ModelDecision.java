package dev.lifeskill.conversation.application.model;

import java.util.Objects;

public record ModelDecision(
        ConversationIntent intent,
        String reply,
        ModelSkillDraftProposal skillDraft,
        String promptVersion) {

    public ModelDecision {
        Objects.requireNonNull(intent, "Conversation intent is required");
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("Model reply must not be blank");
        }
        reply = reply.trim();
        if (intent == ConversationIntent.RECURRING_SKILL && skillDraft == null) {
            throw new IllegalArgumentException("Recurring skill intent requires a skill draft");
        }
        if (intent != ConversationIntent.RECURRING_SKILL && skillDraft != null) {
            throw new IllegalArgumentException("Only recurring skill intent may contain a skill draft");
        }
        if (promptVersion == null || promptVersion.isBlank()) {
            throw new IllegalArgumentException("Prompt version must not be blank");
        }
        promptVersion = promptVersion.trim();
    }
}
