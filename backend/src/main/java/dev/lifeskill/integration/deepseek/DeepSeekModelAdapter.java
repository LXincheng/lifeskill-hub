package dev.lifeskill.integration.deepseek;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.lifeskill.conversation.application.ModelProcessingException;
import dev.lifeskill.conversation.application.model.ModelDecision;
import dev.lifeskill.conversation.application.model.ModelSkillDraftProposal;
import dev.lifeskill.conversation.application.port.ModelPort;

@Component
@ConditionalOnProperty(name = "lifeskill.model.enabled", havingValue = "true")
public class DeepSeekModelAdapter implements ModelPort {

    static final String PROMPT_VERSION = "skill-draft-v1";

    private static final String SYSTEM_PROMPT = """
            你是 LifeSkill Hub 的意图与草案生成组件，只负责理解用户意图和返回结构化数据。
            intent 只能是 ORDINARY、SEARCH、RECURRING_SKILL：
            - ORDINARY：普通讨论或一次性行动建议。
            - SEARCH：需要查询外部最新资料或核对来源。
            - RECURRING_SKILL：包含周期、持续关注、定期整理或重复执行。

            RECURRING_SKILL 必须提供 skillDraft，其他意图 skillDraft 必须为 null。
            当前只支持每周计划；dayOfWeek 使用英文大写星期，time 使用 24 小时 HH:mm，
            timezone 默认 Asia/Shanghai。不要声称已经搜索、创建 Skill、设置通知或执行外部操作。
            reply 使用简洁中文；持续需求只说明已生成待确认草案。
            """;

    private final ChatClient chatClient;

    public DeepSeekModelAdapter(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public ModelDecision analyze(String userMessage) {
        try {
            DeepSeekModelResponse response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(DeepSeekModelResponse.class, spec -> spec.validateSchema());
            if (response == null) {
                throw new ModelProcessingException("Model returned no structured response");
            }
            return toDecision(response);
        } catch (ModelProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ModelProcessingException("Model request or structured output validation failed", exception);
        }
    }

    static ModelDecision toDecision(DeepSeekModelResponse response) {
        ModelSkillDraftProposal proposal = null;
        if (response.skillDraft() != null) {
            proposal = new ModelSkillDraftProposal(
                    response.skillDraft().title(),
                    response.skillDraft().objective(),
                    response.skillDraft().dayOfWeek(),
                    response.skillDraft().time(),
                    response.skillDraft().timezone());
        }
        return new ModelDecision(response.intent(), response.reply(), proposal, PROMPT_VERSION);
    }
}
