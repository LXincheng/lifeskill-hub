package dev.lifeskill.skill.application;

public record UpdateSkillCommand(
        String name,
        String objective,
        String dayOfWeek,
        String time,
        String timezone,
        String status) {
}
