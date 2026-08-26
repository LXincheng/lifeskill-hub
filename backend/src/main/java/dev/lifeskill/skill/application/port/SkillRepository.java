package dev.lifeskill.skill.application.port;

import java.util.Optional;
import java.util.UUID;

import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillVersion;

public interface SkillRepository {

    void save(Skill skill, SkillVersion version);

    void save(Skill skill);

    Optional<Skill> findById(UUID skillId);

    Optional<SkillVersion> findVersion(UUID skillId, int version);
}
