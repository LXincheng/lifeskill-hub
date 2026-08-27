package dev.lifeskill.agent.application;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.lifeskill.skill.application.SkillApplicationService;
import dev.lifeskill.skill.domain.SkillStatus;

@Component
public class AgentRunScheduler {
    private final SkillApplicationService skills;
    private final AgentRunApplicationService runs;
    private final Clock clock;

    public AgentRunScheduler(SkillApplicationService skills, AgentRunApplicationService runs, Clock clock) {
        this.skills = skills;
        this.runs = runs;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${lifeskill.agent.scheduler-delay-ms:60000}")
    public void startDueWeeklySkills() {
        for (var details : skills.list()) {
            if (details.skill().status() != SkillStatus.ACTIVE) continue;
            var schedule = details.version().schedule();
            ZonedDateTime localNow = ZonedDateTime.now(clock).withZoneSameInstant(schedule.timezone());
            if (localNow.getDayOfWeek() != schedule.dayOfWeek()
                    || localNow.getHour() != schedule.time().getHour()
                    || localNow.getMinute() != schedule.time().getMinute()) continue;
            String slot = "%s:%s:%s".formatted(
                    localNow.toLocalDate(), schedule.time(), schedule.timezone().getId());
            try {
                runs.startScheduled(details.skill().id(), slot);
            } catch (DataIntegrityViolationException ignored) {
                // The unique schedule slot is the cross-thread/cross-process idempotency boundary.
            }
        }
    }
}
