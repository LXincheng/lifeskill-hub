package dev.lifeskill.skill.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSkillVersionRepository extends JpaRepository<SkillVersionEntity, UUID> {

    Optional<SkillVersionEntity> findBySkillIdAndVersion(UUID skillId, int version);
}
