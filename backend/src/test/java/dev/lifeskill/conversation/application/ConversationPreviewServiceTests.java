package dev.lifeskill.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationPreviewServiceTests {

    private final ConversationPreviewService service = new ConversationPreviewService();

    @Test
    void proposesSkillDraftForRecurringRequest() {
        var response = service.preview("每周五整理 Java Agent 的重要变化");

        assertThat(response.intent()).isEqualTo("RECURRING_SKILL");
        assertThat(response.skillDraft()).isNotNull();
        assertThat(response.skillDraft().name()).isEqualTo("Java Agent Weekly");
        assertThat(response.skillDraft().schedule()).isEqualTo("FRIDAY 18:00");
        assertThat(response.skillDraft().requiresConfirmation()).isTrue();
    }

    @Test
    void keepsOneOffRequestWithoutCreatingSkill() {
        var response = service.preview("解释 Java Instrumentation 是什么");

        assertThat(response.intent()).isEqualTo("ONE_OFF_REQUEST");
        assertThat(response.skillDraft()).isNull();
    }
}
