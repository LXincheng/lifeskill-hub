package dev.lifeskill.conversation.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.conversation.domain.ConversationSummary;

public record ConversationSummaryResponse(
        UUID id, String title, int messageCount, Instant createdAt, Instant updatedAt) {
    public static ConversationSummaryResponse from(ConversationSummary summary) {
        return new ConversationSummaryResponse(
                summary.id(), summary.title(), summary.messageCount(), summary.createdAt(), summary.updatedAt());
    }
}
