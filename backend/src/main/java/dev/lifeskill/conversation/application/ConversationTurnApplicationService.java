package dev.lifeskill.conversation.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.lifeskill.conversation.application.model.ConversationIntent;
import dev.lifeskill.conversation.application.model.ModelDecision;
import dev.lifeskill.conversation.application.model.ModelSkillDraftProposal;
import dev.lifeskill.conversation.application.port.ModelPort;
import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.conversation.domain.Message;
import dev.lifeskill.shared.application.IdGenerator;
import dev.lifeskill.skill.application.SkillDraftApplicationService;
import dev.lifeskill.skill.domain.SkillDraft;
import dev.lifeskill.skill.domain.SkillDraftStatus;
import dev.lifeskill.skill.domain.WeeklySchedule;

@Service
public class ConversationTurnApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationTurnApplicationService.class);

    static final String MODEL_UNAVAILABLE_REPLY =
            "消息已保存，但 AI 草案暂时不可用：模型服务可能异常，或结果没有通过校验。你可以稍后重试；系统不会据此创建长期任务。";
    static final String SEARCH_NOT_READY_REPLY =
            "我识别到这是一次搜索需求。可靠来源检索将在下一阶段接入，在此之前我不会生成未经核验的搜索结论。";

    private final ConversationApplicationService conversationService;
    private final ConversationCompletionApplicationService completionService;
    private final SkillDraftApplicationService skillDraftService;
    private final ModelPort modelPort;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ConversationTurnApplicationService(
            ConversationApplicationService conversationService,
            ConversationCompletionApplicationService completionService,
            SkillDraftApplicationService skillDraftService,
            ModelPort modelPort,
            Clock clock,
            IdGenerator idGenerator) {
        this.conversationService = conversationService;
        this.completionService = completionService;
        this.skillDraftService = skillDraftService;
        this.modelPort = modelPort;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    public ConversationTurnResult sendMessage(UUID conversationId, String content) {
        Conversation conversation = conversationService.sendUserMessage(conversationId, content);
        Message sourceMessage = conversation.messages().getLast();

        try {
            ModelDecision decision = modelPort.analyze(sourceMessage.content());
            if (decision == null) {
                throw new ModelProcessingException("Model returned no decision");
            }
            Optional<SkillDraft> draft = createDraft(conversationId, sourceMessage.id(), decision);
            String assistantReply = decision.intent() == ConversationIntent.SEARCH
                    ? SEARCH_NOT_READY_REPLY
                    : decision.reply();
            Conversation completed = completionService.complete(conversationId, assistantReply, draft);
            return result(completed);
        } catch (ModelProcessingException | IllegalArgumentException | DateTimeException exception) {
            LOGGER.warn(
                    "Conversation model turn degraded conversationId={} reason={}",
                    conversationId,
                    exception.getClass().getSimpleName());
            Conversation completed = completionService.complete(
                    conversationId,
                    MODEL_UNAVAILABLE_REPLY,
                    Optional.empty());
            return result(completed);
        }
    }

    private Optional<SkillDraft> createDraft(
            UUID conversationId,
            UUID sourceMessageId,
            ModelDecision decision) {
        if (decision.intent() != ConversationIntent.RECURRING_SKILL) {
            return Optional.empty();
        }

        ModelSkillDraftProposal proposal = decision.skillDraft();
        var now = clock.instant();
        return Optional.of(new SkillDraft(
                idGenerator.nextId(),
                conversationId,
                sourceMessageId,
                proposal.title(),
                proposal.objective(),
                new WeeklySchedule(
                        DayOfWeek.valueOf(requireUppercase(proposal.dayOfWeek(), "Schedule day")),
                        LocalTime.parse(requireText(proposal.time(), "Schedule time")),
                        ZoneId.of(requireText(proposal.timezone(), "Schedule timezone"))),
                SkillDraftStatus.PENDING_CONFIRMATION,
                decision.promptVersion(),
                now,
                now));
    }

    private ConversationTurnResult result(Conversation conversation) {
        return new ConversationTurnResult(
                conversation,
                skillDraftService.findByConversationId(conversation.id()));
    }

    private String requireUppercase(String value, String fieldName) {
        return requireText(value, fieldName).toUpperCase(java.util.Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
