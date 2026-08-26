package dev.lifeskill.skill.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SkillVersion(
        UUID id,
        UUID skillId,
        int version,
        String objective,
        WeeklySchedule schedule,
        Instant createdAt) {

    public SkillVersion {
        Objects.requireNonNull(id, "Skill version id is required");
        Objects.requireNonNull(skillId, "Skill id is required");
        if (version < 1) {
            throw new IllegalArgumentException("Skill version must be positive");
        }
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("Skill objective must not be blank");
        }
        objective = objective.trim();
        if (objective.length() > SkillDraft.MAX_OBJECTIVE_LENGTH) {
            throw new IllegalArgumentException(
                    "Skill objective must not exceed " + SkillDraft.MAX_OBJECTIVE_LENGTH + " characters");
        }
        Objects.requireNonNull(schedule, "Skill schedule is required");
        Objects.requireNonNull(createdAt, "Skill version creation time is required");
    }
}
