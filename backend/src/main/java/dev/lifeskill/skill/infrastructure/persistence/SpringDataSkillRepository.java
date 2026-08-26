package dev.lifeskill.skill.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSkillRepository extends JpaRepository<SkillEntity, UUID> {
}
