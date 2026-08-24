package dev.lifeskill.integration.deepseek;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

import dev.lifeskill.conversation.application.model.ConversationIntent;

class DeepSeekModelContractTest {

    @Test
    void convertsTheFixedRecurringSkillSampleIntoTheModelPortContract() {
        String modelJson = """
                {
                  "intent": "RECURRING_SKILL",
                  "reply": "我整理了一份待确认的 Skill 草案。",
                  "skillDraft": {
                    "title": "Java Agent Weekly",
                    "objective": "每周整理 Java Agent 前沿，并优先核对官方来源。",
                    "dayOfWeek": "FRIDAY",
                    "time": "09:00",
                    "timezone": "Asia/Shanghai"
                  }
                }
                """;

        DeepSeekModelResponse response = new BeanOutputConverter<>(DeepSeekModelResponse.class).convert(modelJson);
        var decision = DeepSeekModelAdapter.toDecision(response);

        assertThat(decision.intent()).isEqualTo(ConversationIntent.RECURRING_SKILL);
        assertThat(decision.skillDraft().dayOfWeek()).isEqualTo("FRIDAY");
        assertThat(decision.promptVersion()).isEqualTo("skill-draft-v1");
    }

    @Test
    void convertsOrdinaryAndSearchSamplesWithoutInventingDrafts() {
        var converter = new BeanOutputConverter<>(DeepSeekModelResponse.class);
        var ordinary = DeepSeekModelAdapter.toDecision(converter.convert("""
                {"intent":"ORDINARY","reply":"可以先把目标拆成今天的一步。","skillDraft":null}
                """));
        var search = DeepSeekModelAdapter.toDecision(converter.convert("""
                {"intent":"SEARCH","reply":"这需要查询最新官方资料。","skillDraft":null}
                """));

        assertThat(ordinary.intent()).isEqualTo(ConversationIntent.ORDINARY);
        assertThat(ordinary.skillDraft()).isNull();
        assertThat(search.intent()).isEqualTo(ConversationIntent.SEARCH);
        assertThat(search.skillDraft()).isNull();
    }
}
