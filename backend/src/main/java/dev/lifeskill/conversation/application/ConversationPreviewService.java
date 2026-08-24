package dev.lifeskill.conversation.application;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.lifeskill.conversation.api.dto.ConversationPreviewResponse;
import dev.lifeskill.conversation.api.dto.ConversationPreviewResponse.AgentEvent;
import dev.lifeskill.conversation.api.dto.ConversationPreviewResponse.SkillDraft;

@Service
public class ConversationPreviewService {

    public ConversationPreviewResponse preview(String message) {
        boolean recurring = containsAny(message, "每周", "每天", "持续", "定期", "提醒");

        if (!recurring) {
            return new ConversationPreviewResponse(
                    "ONE_OFF_REQUEST",
                    "我会先把它作为一次性请求处理；如果你希望持续执行，可以再保存为 Skill。",
                    List.of(new AgentEvent("PLANNING", "识别一次性需求", "COMPLETED")),
                    null);
        }

        return new ConversationPreviewResponse(
                "RECURRING_SKILL",
                "这是一个持续性需求。我已生成可编辑的 Skill 草案，确认后才会创建调度和推送。",
                List.of(
                        new AgentEvent("PLANNING", "拆分关注主题与执行条件", "COMPLETED"),
                        new AgentEvent("COLLECTING", "确认可用的一手来源", "COMPLETED"),
                        new AgentEvent("VERIFYING", "建立来源与核验策略", "COMPLETED"),
                        new AgentEvent("PERSISTING", "等待用户确认后落库", "WAITING")),
                new SkillDraft(
                        inferName(message),
                        inferSchedule(message),
                        "5_MIN_READ",
                        List.of("PREFER_PRIMARY_SOURCE", "VERIFY_IMPORTANT_CLAIMS"),
                        true));
    }

    private boolean containsAny(String source, String... candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String inferName(String message) {
        if (message.toLowerCase().contains("java agent")) {
            return "Java Agent Weekly";
        }
        return "Personal Watch Skill";
    }

    private String inferSchedule(String message) {
        if (message.contains("每周五")) {
            return "FRIDAY 18:00";
        }
        if (message.contains("每天")) {
            return "DAILY 18:00";
        }
        return "WEEKLY";
    }
}
