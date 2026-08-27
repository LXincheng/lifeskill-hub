package dev.lifeskill.skill.application;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.shared.application.IdGenerator;
import dev.lifeskill.skill.application.port.SkillRepository;
import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillStatus;
import dev.lifeskill.skill.domain.SkillVersion;
import dev.lifeskill.skill.domain.WeeklySchedule;

@Service
public class SkillApplicationService {

    private final SkillRepository repository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SkillApplicationService(SkillRepository repository, Clock clock, IdGenerator idGenerator) {
        this.repository = repository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public SkillDetails get(UUID skillId) {
        Skill skill = findSkill(skillId);
        return new SkillDetails(skill, findVersion(skill));
    }

    @Transactional(readOnly = true)
    public List<SkillDetails> list() {
        return repository.findAll().stream().map(skill -> new SkillDetails(skill, findVersion(skill))).toList();
    }

    @Transactional
    public SkillDetails update(UUID skillId, UpdateSkillCommand command) {
        Skill skill = findSkill(skillId);
        SkillVersion currentVersion = findVersion(skill);
        var now = clock.instant();

        SkillStatus nextStatus = parseStatus(command.status(), skill.status());
        String nextName = textOrCurrent(command.name(), skill.name());
        String nextObjective = textOrCurrent(command.objective(), currentVersion.objective());
        WeeklySchedule nextSchedule = mergeSchedule(currentVersion.schedule(), command);
        boolean configChanged = !nextObjective.equals(currentVersion.objective())
                || !nextSchedule.equals(currentVersion.schedule());
        boolean descriptionChanged = !nextObjective.equals(skill.description());
        boolean nameChanged = !nextName.equals(skill.name());

        Skill updated = skill.changeStatus(nextStatus, now);
        SkillVersion resultVersion = currentVersion;
        if (configChanged) {
            int nextVersion = skill.currentVersion() + 1;
            updated = updated.revise(nextName, nextObjective, nextVersion, now);
            resultVersion = new SkillVersion(
                    idGenerator.nextId(), skill.id(), nextVersion, nextObjective, nextSchedule, now);
            repository.save(updated, resultVersion);
        } else if (nameChanged || descriptionChanged) {
            updated = updated.revise(nextName, nextObjective, skill.currentVersion(), now);
            repository.save(updated);
        } else if (updated != skill) {
            repository.save(updated);
        }
        return new SkillDetails(updated, resultVersion);
    }

    private Skill findSkill(UUID skillId) {
        return repository.findById(skillId).orElseThrow(() -> new SkillNotFoundException(skillId));
    }

    private SkillVersion findVersion(Skill skill) {
        return repository.findVersion(skill.id(), skill.currentVersion())
                .orElseThrow(() -> new IllegalStateException("Current skill version is missing"));
    }

    private SkillStatus parseStatus(String value, SkillStatus current) {
        if (value == null) return current;
        try {
            return SkillStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidSkillUpdateException("Skill status must be ACTIVE or PAUSED");
        }
    }

    private WeeklySchedule mergeSchedule(WeeklySchedule current, UpdateSkillCommand command) {
        try {
            DayOfWeek day = command.dayOfWeek() == null
                    ? current.dayOfWeek()
                    : DayOfWeek.valueOf(command.dayOfWeek().trim().toUpperCase());
            LocalTime time = command.time() == null ? current.time() : LocalTime.parse(command.time().trim());
            ZoneId timezone = command.timezone() == null
                    ? current.timezone()
                    : ZoneId.of(command.timezone().trim());
            return new WeeklySchedule(day, time, timezone);
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new InvalidSkillUpdateException("Schedule contains an invalid day, time, or timezone");
        }
    }

    private String textOrCurrent(String value, String current) {
        return value == null ? current : value;
    }
}
