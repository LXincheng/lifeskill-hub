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
import dev.lifeskill.agent.application.AgentRunApplicationService;
import dev.lifeskill.agent.domain.ResearchCapability;
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
            "我识别到这是一次研究需求，但当前没有与该主题匹配的可靠来源适配器。系统不会用通用模型编造最新事实。";
    static final String TICKET_NOT_READY_REPLY =
            "我理解你想持续监控场次和皇帝座，但当前尚未接入正大乐影城的官方场次、库存和锁座工具，因此不能诚实地创建一个会自动抢票的 Skill。这个任务需要：影院只读查询适配器、登录授权、开售前重新核验，以及锁座/下单前由你再次确认；支付不会自动执行。";
    static final String RESEARCH_STARTED_REPLY =
            "已启动一次性官方研究。系统会采集 World Gold Council 最新研究，保存 Evidence，独立核验后生成一份可追溯的专业报告。你可以在下方实时查看步骤，完成后直接打开报告。";
    static final String LEARNING_PLAN_STARTED_REPLY =
            "已开始为这个目标设计学习系统。Planner 会拆解目标，Curriculum Designer 生成路径、导读与测验，Java Learning Gate 会在保存前检查结构。完成后可直接进入学习空间并持续记录进度。";

    private final ConversationApplicationService conversationService;
    private final ConversationCompletionApplicationService completionService;
    private final SkillDraftApplicationService skillDraftService;
    private final ModelPort modelPort;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final AgentRunApplicationService agentRuns;

    public ConversationTurnApplicationService(
            ConversationApplicationService conversationService,
            ConversationCompletionApplicationService completionService,
            SkillDraftApplicationService skillDraftService,
            ModelPort modelPort,
            Clock clock,
            IdGenerator idGenerator,
            AgentRunApplicationService agentRuns) {
        this.conversationService = conversationService;
        this.completionService = completionService;
        this.skillDraftService = skillDraftService;
        this.modelPort = modelPort;
        this.clock = clock;
        this.idGenerator = idGenerator;
        this.agentRuns = agentRuns;
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
            ResearchCapability capability = ResearchCapability.detect(sourceMessage.content());
            if (capability == ResearchCapability.TICKET_ASSIST) {
                return completeUnavailableTicket(conversationId, steps, turnStartedAt);
            }
            if (capability.isLearningPlan()) {
                return startLearningPlan(conversationId, sourceMessage, steps, turnStartedAt);
            }
            // 已接入的官方研究使用确定性路由，避免模型把“做一份报告”误判成闲聊而跳过真实工具链。
            if (capability.isRunnableResearch()
                    && !ResearchCapability.requestsRecurringExecution(sourceMessage.content())) {
                return startOfficialResearch(conversationId, sourceMessage, capability, steps, turnStartedAt);
            }

            long modelStartedAt = System.nanoTime();
            ModelDecision decision = modelPort.analyze(sourceMessage.content());
            if (decision == null) {
                throw new ModelProcessingException("Model returned no decision");
            }
            steps.add(new ProcessingStep(
                    "PLANNING", "意图与草案分析", ProcessingStepStatus.COMPLETED,
                    elapsedMs(modelStartedAt), "结构化输出已通过 Schema 校验"));
            Optional<SkillDraft> draft = createDraft(conversationId, sourceMessage.id(), decision);
            UUID agentRunId = null;
            if (decision.intent() == ConversationIntent.RECURRING_SKILL) {
                steps.add(new ProcessingStep(
                        "POLICY_CHECK", "长期任务规则校验", ProcessingStepStatus.COMPLETED,
                        0, "周期、时区和确认边界已通过 Java 规则校验"));
            } else if (decision.intent() == ConversationIntent.SEARCH) {
                if (capability.isRunnableResearch()) {
                    agentRunId = agentRuns.startResearch(
                            conversationId, sourceMessage.id(), sourceMessage.content(), capability).run().id();
                    steps.add(new ProcessingStep(
                            "COLLECTING", "官方研究已排队", ProcessingStepStatus.COMPLETED,
                            0, "AgentRun 已创建；后续步骤通过流式事件持续更新"));
                } else {
                    steps.add(new ProcessingStep(
                            "COLLECTING", "可靠来源收集", ProcessingStepStatus.BLOCKED,
                            0, "没有匹配的官方 Source Adapter，未生成未经核验的结论"));
                }
            }
            String assistantReply = agentRunId != null ? RESEARCH_STARTED_REPLY
                    : decision.intent() == ConversationIntent.SEARCH ? SEARCH_NOT_READY_REPLY : decision.reply();
            steps.add(new ProcessingStep(
                    "COMPOSING", "回复已整理", ProcessingStepStatus.COMPLETED,
                    0, "仅展示最终答复，不包含模型原始思维链"));
            Conversation completed = completionService.complete(
                    conversationId, assistantReply, draft, steps, elapsedMs(turnStartedAt), agentRunId);
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

    private ConversationTurnResult startOfficialResearch(
            UUID conversationId,
            Message sourceMessage,
            ResearchCapability capability,
            List<ProcessingStep> steps,
            long turnStartedAt) {
        steps.add(new ProcessingStep(
                "PLANNING", "官方研究路由", ProcessingStepStatus.COMPLETED,
                0, "已由 Java 规则匹配可用 Source Adapter"));
        UUID agentRunId = agentRuns.startResearch(
                conversationId, sourceMessage.id(), sourceMessage.content(), capability).run().id();
        steps.add(new ProcessingStep(
                "COLLECTING", "官方研究已排队", ProcessingStepStatus.COMPLETED,
                0, "AgentRun 已创建；后续步骤通过流式事件持续更新"));
        steps.add(new ProcessingStep(
                "COMPOSING", "回复已整理", ProcessingStepStatus.COMPLETED,
                0, "仅展示最终答复，不包含模型原始思维链"));
        return result(completionService.complete(
                conversationId, RESEARCH_STARTED_REPLY, Optional.empty(), steps,
                elapsedMs(turnStartedAt), agentRunId));
    }

    private ConversationTurnResult startLearningPlan(
            UUID conversationId,
            Message sourceMessage,
            List<ProcessingStep> steps,
            long turnStartedAt) {
        steps.add(new ProcessingStep(
                "PLANNING", "学习目标路由", ProcessingStepStatus.COMPLETED,
                0, "已匹配个性化学习 Harness，不创建空白模板"));
        UUID agentRunId = agentRuns.startLearningPlan(
                conversationId, sourceMessage.id(), sourceMessage.content()).run().id();
        steps.add(new ProcessingStep(
                "COMPOSING", "课程设计已排队", ProcessingStepStatus.COMPLETED,
                0, "AgentRun 已创建；路径、文章与测验会作为同一学习文件夹保存"));
        return result(completionService.complete(
                conversationId, LEARNING_PLAN_STARTED_REPLY, Optional.empty(), steps,
                elapsedMs(turnStartedAt), agentRunId));
    }

    private ConversationTurnResult completeUnavailableTicket(
            UUID conversationId,
            List<ProcessingStep> steps,
            long turnStartedAt) {
        steps.add(new ProcessingStep(
                "POLICY_CHECK", "外部操作能力检查", ProcessingStepStatus.BLOCKED,
                0, "缺少影院官方查询、库存、锁座和下单 Tool，未创建不可执行的假任务"));
        steps.add(new ProcessingStep(
                "COMPOSING", "安全边界已说明", ProcessingStepStatus.COMPLETED,
                0, "锁座和下单必须在实时复核后由用户确认，支付不会自动执行"));
        return result(completionService.complete(
                conversationId, TICKET_NOT_READY_REPLY, Optional.empty(), steps, elapsedMs(turnStartedAt)));
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
