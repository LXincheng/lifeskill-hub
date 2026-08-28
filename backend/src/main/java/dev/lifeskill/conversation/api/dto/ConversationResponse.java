package dev.lifeskill.conversation.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.conversation.domain.Message;
import dev.lifeskill.conversation.domain.MessageRole;
import dev.lifeskill.conversation.domain.ProcessingStep;
import dev.lifeskill.skill.domain.SkillDraft;

public record ConversationResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages,
        List<SkillDraftResponse> skillDrafts) {

    public static ConversationResponse from(Conversation conversation) {
        return from(conversation, List.of());
    }

    public static ConversationResponse from(Conversation conversation, List<SkillDraft> skillDrafts) {
        return new ConversationResponse(
                conversation.id(),
                conversation.title(),
                conversation.createdAt(),
                conversation.updatedAt(),
                conversation.messages().stream().map(MessageResponse::from).toList(),
                skillDrafts.stream().map(SkillDraftResponse::from).toList());
    }

    public record MessageResponse(
            UUID id,
            MessageRole role,
            String content,
            Instant createdAt,
            List<ProcessingStep> processingSteps,
            Long durationMs,
            UUID agentRunId) {

        private static MessageResponse from(Message message) {
            return new MessageResponse(
                    message.id(),
                    message.role(),
                    message.content(),
                    message.createdAt(),
                    message.processingSteps(),
                    message.durationMs(),
                    message.agentRunId());
        }
    }

    public record SkillDraftResponse(
            UUID id,
            UUID sourceMessageId,
            String title,
            String objective,
            String dayOfWeek,
            String time,
            String timezone,
            String status,
            UUID confirmedSkillId,
            Instant confirmedAt,
            Instant createdAt) {

        private static SkillDraftResponse from(SkillDraft draft) {
            return new SkillDraftResponse(
                    draft.id(),
                    draft.sourceMessageId(),
                    draft.title(),
                    draft.objective(),
                    draft.schedule().dayOfWeek().name(),
                    draft.schedule().time().toString(),
                    draft.schedule().timezone().getId(),
                    draft.status().name(),
                    draft.confirmedSkillId(),
                    draft.confirmedAt(),
                    draft.createdAt());
        }
    }
}
