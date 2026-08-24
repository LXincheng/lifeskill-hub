package dev.lifeskill.conversation.api.dto;

import java.util.List;

public record ConversationPreviewResponse(
        String intent,
        String assistantMessage,
        List<AgentEvent> events,
        SkillDraft skillDraft) {

    public record AgentEvent(String stage, String label, String status) {
    }

    public record SkillDraft(
            String name,
            String schedule,
            String outputLength,
            List<String> sourcePolicy,
            boolean requiresConfirmation) {
    }
}
