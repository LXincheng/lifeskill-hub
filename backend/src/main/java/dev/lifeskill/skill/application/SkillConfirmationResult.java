package dev.lifeskill.skill.application;

import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillDraft;
import dev.lifeskill.skill.domain.SkillVersion;

public record SkillConfirmationResult(
        SkillDraft draft,
        Skill skill,
        SkillVersion version,
        boolean created) {
}
