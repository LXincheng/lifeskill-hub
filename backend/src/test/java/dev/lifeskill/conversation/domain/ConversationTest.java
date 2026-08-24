package dev.lifeskill.conversation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConversationTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    @Test
    void usesFirstMessageAsAReadableConversationTitle() {
        Conversation conversation = Conversation.start(UUID.randomUUID(), NOW);

        conversation.addUserMessage(UUID.randomUUID(), "  每周五整理 Java Agent 的重要变化  ", NOW.plusSeconds(1));

        assertThat(conversation.title()).isEqualTo("每周五整理 Java Agent 的重要变化");
        assertThat(conversation.messages()).singleElement().satisfies(message -> {
            assertThat(message.role()).isEqualTo(MessageRole.USER);
            assertThat(message.content()).isEqualTo("每周五整理 Java Agent 的重要变化");
        });
        assertThat(conversation.updatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void shortensLongTitlesWithoutBreakingUnicodeCodePoints() {
        Conversation conversation = Conversation.start(UUID.randomUUID(), NOW);

        conversation.addUserMessage(UUID.randomUUID(), "😀".repeat(40), NOW);

        assertThat(conversation.title()).isEqualTo("😀".repeat(32) + "…");
    }

    @Test
    void rejectsBlankMessages() {
        Conversation conversation = Conversation.start(UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> conversation.addUserMessage(UUID.randomUUID(), "  ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message content must not be blank");
    }
}
