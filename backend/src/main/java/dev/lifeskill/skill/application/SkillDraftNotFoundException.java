package dev.lifeskill.skill.application;

import java.util.UUID;

public class SkillDraftNotFoundException extends RuntimeException {

    public SkillDraftNotFoundException(UUID draftId) {
        super("Skill draft %s was not found".formatted(draftId));
    }
}
