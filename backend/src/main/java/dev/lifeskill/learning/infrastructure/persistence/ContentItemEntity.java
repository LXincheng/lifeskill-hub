package dev.lifeskill.learning.infrastructure.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.lifeskill.learning.domain.ContentItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_item")
class ContentItemEntity {
    @Id
    private UUID id;
    @Column(name = "folder_id", nullable = false)
    private UUID folderId;
    @Column(name = "source_skill_run_id")
    private UUID sourceSkillRunId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentItemType type;
    @Column(nullable = false, length = 240)
    private String title;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
    @Column(name = "verification_status", nullable = false, length = 32)
    private String verificationStatus;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentItemEntity() {}

    ContentItemEntity(
            UUID id,
            UUID folderId,
            ContentItemType type,
            String title,
            Map<String, Object> payload,
            Instant createdAt,
            Instant updatedAt) {
        this(id, folderId, null, type, title, payload, "USER_AUTHORED", createdAt, updatedAt);
    }

    ContentItemEntity(
            UUID id,
            UUID folderId,
            UUID sourceSkillRunId,
            ContentItemType type,
            String title,
            Map<String, Object> payload,
            String verificationStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.folderId = folderId;
        this.sourceSkillRunId = sourceSkillRunId;
        this.type = type;
        this.title = title;
        this.payload = payload;
        this.verificationStatus = verificationStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID id() { return id; }
    UUID folderId() { return folderId; }
    UUID sourceSkillRunId() { return sourceSkillRunId; }
    ContentItemType type() { return type; }
    String title() { return title; }
    Map<String, Object> payload() { return payload; }
    String verificationStatus() { return verificationStatus; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
