package dev.lifeskill.agent.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResearchCapabilityTest {
    @Test
    void routesGoldAndTicketRequestsToDifferentSafetyBoundaries() {
        assertThat(ResearchCapability.detect("帮我做一份9月份黄金报告"))
                .isEqualTo(ResearchCapability.GOLD_MARKET);
        assertThat(ResearchCapability.detect("盯着上海正大乐影城的皇帝座电影票"))
                .isEqualTo(ResearchCapability.TICKET_ASSIST);
        assertThat(ResearchCapability.detect("为我创建一个 Java Agent 开发学习路径"))
                .isEqualTo(ResearchCapability.LEARNING_PLAN);
        assertThat(ResearchCapability.GOLD_MARKET.isRunnableResearch()).isTrue();
        assertThat(ResearchCapability.TICKET_ASSIST.isRunnableResearch()).isFalse();
        assertThat(ResearchCapability.LEARNING_PLAN.isLearningPlan()).isTrue();
        assertThat(ResearchCapability.requestsRecurringExecution("每周持续关注黄金变化")).isTrue();
        assertThat(ResearchCapability.requestsRecurringExecution("帮我做一份黄金报告")).isFalse();
    }
}
