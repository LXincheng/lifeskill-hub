package dev.lifeskill.conversation.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
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
import dev.lifeskill.conversation.domain.ProcessingStep;
import dev.lifeskill.conversation.domain.ProcessingStepStatus;
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
        long turnStartedAt = System.nanoTime();
        Conversation conversation = conversationService.sendUserMessage(conversationId, content);
        Message sourceMessage = conversation.messages().getLast();
        List<ProcessingStep> steps = new ArrayList<>();
        steps.add(new ProcessingStep(
                "RECEIVED", "消息已保存", ProcessingStepStatus.COMPLETED,
                elapsedMs(turnStartedAt), "已写入对话历史"));

        try {
            long modelStartedAt = System.nanoTime();
            ModelDecision decision = modelPort.analyze(sourceMessage.content());
            if (decision == null) {
                throw new ModelProcessingException("Model returned no decision");
            }
            steps.add(new ProcessingStep(
                    "PLANNING", "意图与草案分析", ProcessingStepStatus.COMPLETED,
                    elapsedMs(modelStartedAt), "结构化输出已通过 Schema 校验"));
            Optional<SkillDraft> draft = createDraft(conversationId, sourceMessage.id(), decision);
            if (decision.intent() == ConversationIntent.RECURRING_SKILL) {
                steps.add(new ProcessingStep(
                        "POLICY_CHECK", "长期任务规则校验", ProcessingStepStatus.COMPLETED,
                        0, "周期、时区和确认边界已通过 Java 规则校验"));
            } else if (decision.intent() == ConversationIntent.SEARCH) {
                steps.add(new ProcessingStep(
                        "COLLECTING", "可靠来源收集", ProcessingStepStatus.BLOCKED,
                        0, "Source Adapter 与 Evidence 流水线尚未接入，未生成未经核验的结论"));
            }
            String assistantReply = decision.intent() == ConversationIntent.SEARCH
                    ? SEARCH_NOT_READY_REPLY
                    : decision.reply();
            steps.add(new ProcessingStep(
                    "COMPOSING", "回复已整理", ProcessingStepStatus.COMPLETED,
                    0, "仅展示最终答复，不包含模型原始思维链"));
            Conversation completed = completionService.complete(
                    conversationId, assistantReply, draft, steps, elapsedMs(turnStartedAt));
            return result(completed);
        } catch (ModelProcessingException | IllegalArgumentException | DateTimeException exception) {
            LOGGER.warn(
                    "Conversation model turn degraded conversationId={} reason={}",
                    conversationId,
                    exception.getClass().getSimpleName());
            steps.add(new ProcessingStep(
                    "PLANNING", "模型响应校验", ProcessingStepStatus.FAILED,
                    elapsedMs(turnStartedAt), "模型服务异常或结构化输出未通过校验，已安全降级"));
            Conversation completed = completionService.complete(
                    conversationId, MODEL_UNAVAILABLE_REPLY, Optional.empty(), steps, elapsedMs(turnStartedAt));
            return result(completed);
        }
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
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
                null,
                null,
                null,
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
