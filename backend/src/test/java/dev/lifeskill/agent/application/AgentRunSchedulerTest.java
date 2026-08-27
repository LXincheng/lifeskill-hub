package dev.lifeskill.agent.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.lifeskill.skill.application.SkillApplicationService;
import dev.lifeskill.skill.application.SkillDetails;
import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillStatus;
import dev.lifeskill.skill.domain.SkillVersion;
import dev.lifeskill.skill.domain.WeeklySchedule;

@ExtendWith(MockitoExtension.class)
class AgentRunSchedulerTest {
    @Mock SkillApplicationService skills;
    @Mock AgentRunApplicationService runs;

    @Test
    void startsOnlyTheActiveSkillMatchingItsLocalWeeklyMinute() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T01:00:00Z"), ZoneId.of("UTC"));
        UUID dueId = UUID.randomUUID();
        UUID pausedId = UUID.randomUUID();
        when(skills.list()).thenReturn(List.of(
                details(dueId, SkillStatus.ACTIVE, DayOfWeek.FRIDAY, LocalTime.of(9, 0)),
                details(pausedId, SkillStatus.PAUSED, DayOfWeek.FRIDAY, LocalTime.of(9, 0))));

        new AgentRunScheduler(skills, runs, clock).startDueWeeklySkills();

        verify(runs).startScheduled(dueId, "2026-08-28:09:00:Asia/Shanghai");
        verify(runs, never()).startScheduled(org.mockito.ArgumentMatchers.eq(pausedId), org.mockito.ArgumentMatchers.anyString());
    }

    private SkillDetails details(UUID id, SkillStatus status, DayOfWeek day, LocalTime time) {
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        return new SkillDetails(
                new Skill(id, UUID.randomUUID(), "Weekly", "Official updates", status, 1, now, now),
                new SkillVersion(UUID.randomUUID(), id, 1, "Official updates",
                        new WeeklySchedule(day, time, ZoneId.of("Asia/Shanghai")), now));
    }
}
