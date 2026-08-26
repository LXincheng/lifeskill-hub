package dev.lifeskill.learning.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_folder")
class LearningFolderEntity {
    @Id
    private UUID id;
    @Column(name = "parent_id")
    private UUID parentId;
    @Column(nullable = false, length = 160)
    private String name;
    @Column
    private String description;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LearningFolderEntity() {}

    LearningFolderEntity(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID id() { return id; }
    String name() { return name; }
    String description() { return description; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
