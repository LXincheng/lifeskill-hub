package dev.lifeskill.skill.api.dto;

import dev.lifeskill.skill.application.UpdateSkillCommand;
import jakarta.validation.constraints.Size;

public record UpdateSkillRequest(
        @Size(min = 1, max = 120) String name,
        @Size(min = 1, max = 1000) String objective,
        String dayOfWeek,
        String time,
        @Size(min = 1, max = 60) String timezone,
        String status) {

    public UpdateSkillCommand toCommand() {
        return new UpdateSkillCommand(name, objective, dayOfWeek, time, timezone, status);
    }
}
