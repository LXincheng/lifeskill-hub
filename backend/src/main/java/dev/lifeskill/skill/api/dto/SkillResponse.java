package dev.lifeskill.skill.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.skill.application.SkillDetails;

public record SkillResponse(
        UUID id,
        String name,
        String objective,
        String status,
        int currentVersion,
        String dayOfWeek,
        String time,
        String timezone,
        Instant createdAt,
        Instant updatedAt) {

    public static SkillResponse from(SkillDetails details) {
        var skill = details.skill();
        var version = details.version();
        return new SkillResponse(
                skill.id(),
                skill.name(),
                version.objective(),
                skill.status().name(),
                skill.currentVersion(),
                version.schedule().dayOfWeek().name(),
                version.schedule().time().toString(),
                version.schedule().timezone().getId(),
                skill.createdAt(),
                skill.updatedAt());
    }
}
