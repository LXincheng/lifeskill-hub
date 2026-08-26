package dev.lifeskill.skill.infrastructure.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "skill_version",
        uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "version"}))
class SkillVersionEntity {

    @Id
    private UUID id;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "version", nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SkillVersionEntity() {
    }

    SkillVersionEntity(
            UUID id,
            UUID skillId,
            int version,
            Map<String, Object> config,
            Instant createdAt) {
        this.id = id;
        this.skillId = skillId;
        this.version = version;
        this.config = Map.copyOf(config);
        this.createdAt = createdAt;
    }

    UUID id() { return id; }
    UUID skillId() { return skillId; }
    int version() { return version; }
    Map<String, Object> config() { return config; }
    Instant createdAt() { return createdAt; }
}
