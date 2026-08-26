package dev.lifeskill.skill.application;

import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillVersion;

public record SkillDetails(Skill skill, SkillVersion version) {
}
