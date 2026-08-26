package dev.lifeskill.skill.application;

import java.util.UUID;

public class SkillNotFoundException extends RuntimeException {

    public SkillNotFoundException(UUID skillId) {
        super("Skill %s was not found".formatted(skillId));
    }
}
