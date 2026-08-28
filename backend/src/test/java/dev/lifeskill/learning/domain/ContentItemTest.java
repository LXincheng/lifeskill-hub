package dev.lifeskill.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ContentItemTest {
    @Test
    void downgradesVerificationWhenAUserEditsGeneratedContent() {
        Instant now = Instant.parse("2026-08-28T08:00:00Z");
        ContentItem verified = new ContentItem(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ContentItemType.REPORT,
                "官方报告", "核验后的正文", "VERIFIED", now, now);
        ContentItem generated = new ContentItem(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ContentItemType.ARTICLE,
                "AI 导读", "模型生成的正文", "AI_GENERATED", now, now);

        assertThat(verified.update(null, null, "人工增加的观点", now.plusSeconds(1)).verificationStatus())
                .isEqualTo("PARTIALLY_VERIFIED");
        assertThat(generated.update(null, null, "人工重写的正文", now.plusSeconds(1)).verificationStatus())
                .isEqualTo("USER_AUTHORED");
    }
}
