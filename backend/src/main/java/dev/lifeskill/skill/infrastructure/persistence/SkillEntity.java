package dev.lifeskill.skill.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.skill.domain.SkillStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill")
class SkillEntity {

    @Id
    private UUID id;

    @Column(name = "source_draft_id", nullable = false, unique = true)
    private UUID sourceDraftId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private SkillStatus status;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillEntity() {
    }

    SkillEntity(
            UUID id,
            UUID sourceDraftId,
            String name,
            String description,
            SkillStatus status,
            int currentVersion,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.sourceDraftId = sourceDraftId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.currentVersion = currentVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID id() { return id; }
    UUID sourceDraftId() { return sourceDraftId; }
    String name() { return name; }
    String description() { return description; }
    SkillStatus status() { return status; }
    int currentVersion() { return currentVersion; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
