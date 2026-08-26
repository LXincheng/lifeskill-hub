package dev.lifeskill.skill.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.lifeskill.skill.application.port.SkillRepository;
import dev.lifeskill.skill.domain.Skill;
import dev.lifeskill.skill.domain.SkillVersion;
import dev.lifeskill.skill.domain.WeeklySchedule;

@Repository
class JpaSkillRepository implements SkillRepository {

    private final SpringDataSkillRepository skillRepository;
    private final SpringDataSkillVersionRepository versionRepository;

    JpaSkillRepository(
            SpringDataSkillRepository skillRepository,
            SpringDataSkillVersionRepository versionRepository) {
        this.skillRepository = skillRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    public void save(Skill skill, SkillVersion version) {
        save(skill);
        versionRepository.save(new SkillVersionEntity(
                version.id(),
                version.skillId(),
                version.version(),
                Map.of(
                        "objective", version.objective(),
                        "dayOfWeek", version.schedule().dayOfWeek().name(),
                        "time", version.schedule().time().toString(),
                        "timezone", version.schedule().timezone().getId()),
                version.createdAt()));
    }

    @Override
    public void save(Skill skill) {
        skillRepository.save(new SkillEntity(
                skill.id(),
                skill.sourceDraftId(),
                skill.name(),
                skill.description(),
                skill.status(),
                skill.currentVersion(),
                skill.createdAt(),
                skill.updatedAt()));
    }

    @Override
    public Optional<Skill> findById(UUID skillId) {
        return skillRepository.findById(skillId).map(this::toDomain);
    }

    @Override
    public Optional<SkillVersion> findVersion(UUID skillId, int version) {
        return versionRepository.findBySkillIdAndVersion(skillId, version).map(this::toDomain);
    }

    private Skill toDomain(SkillEntity entity) {
        return new Skill(
                entity.id(),
                entity.sourceDraftId(),
                entity.name(),
                entity.description(),
                entity.status(),
                entity.currentVersion(),
                entity.createdAt(),
                entity.updatedAt());
    }

    private SkillVersion toDomain(SkillVersionEntity entity) {
        Map<String, Object> config = entity.config();
        return new SkillVersion(
                entity.id(),
                entity.skillId(),
                entity.version(),
                required(config, "objective"),
                new WeeklySchedule(
                        DayOfWeek.valueOf(required(config, "dayOfWeek")),
                        LocalTime.parse(required(config, "time")),
                        ZoneId.of(required(config, "timezone"))),
                entity.createdAt());
    }

    private String required(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Skill version config is missing " + key);
        }
        return text;
    }
}
