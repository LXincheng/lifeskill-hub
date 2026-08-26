package dev.lifeskill.skill.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SkillDraftTest {

    @Test
    void normalizesTextAndKeepsAnExplicitWeeklySchedule() {
        SkillDraft draft = draft("  Java Agent Weekly  ", "  每周核对官方来源  ");

        assertThat(draft.title()).isEqualTo("Java Agent Weekly");
        assertThat(draft.objective()).isEqualTo("每周核对官方来源");
        assertThat(draft.schedule().dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(draft.status()).isEqualTo(SkillDraftStatus.PENDING_CONFIRMATION);
    }

    @Test
    void rejectsBlankOrOversizedModelFields() {
        assertThatThrownBy(() -> draft(" ", "目标"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(() -> draft("x".repeat(121), "目标"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("120");
    }

    @Test
    void confirmsOnceAndKeepsTheOriginalConfirmationOnRetry() {
        SkillDraft pending = draft("Java Agent Weekly", "每周核对官方来源");
        UUID skillId = UUID.randomUUID();
        Instant confirmedAt = Instant.parse("2026-08-24T11:00:00Z");

        SkillDraft confirmed = pending.confirm(skillId, "request-123", confirmedAt);
        SkillDraft retried = confirmed.confirm(UUID.randomUUID(), "another-request", confirmedAt.plusSeconds(10));

        assertThat(confirmed.status()).isEqualTo(SkillDraftStatus.CONFIRMED);
        assertThat(confirmed.confirmedSkillId()).isEqualTo(skillId);
        assertThat(confirmed.confirmationKey()).isEqualTo("request-123");
        assertThat(confirmed.confirmedAt()).isEqualTo(confirmedAt);
        assertThat(retried).isEqualTo(confirmed);
    }

    private SkillDraft draft(String title, String objective) {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        return new SkillDraft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                title,
                objective,
                new WeeklySchedule(DayOfWeek.FRIDAY, LocalTime.of(9, 0), ZoneId.of("Asia/Shanghai")),
                SkillDraftStatus.PENDING_CONFIRMATION,
                "test-v1",
                null,
                null,
                null,
                now,
                now);
    }
}
