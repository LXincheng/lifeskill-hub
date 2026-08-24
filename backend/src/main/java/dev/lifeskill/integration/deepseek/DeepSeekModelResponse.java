package dev.lifeskill.integration.deepseek;

import dev.lifeskill.conversation.application.model.ConversationIntent;

public record DeepSeekModelResponse(
        ConversationIntent intent,
        String reply,
        Draft skillDraft) {

    public record Draft(
            String title,
            String objective,
            String dayOfWeek,
            String time,
            String timezone) {
    }
}
