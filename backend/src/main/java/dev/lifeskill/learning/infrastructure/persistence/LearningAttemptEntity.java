package dev.lifeskill.learning.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.lifeskill.learning.domain.LearningAttemptKind;
import dev.lifeskill.learning.domain.LearningAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_attempt")
class LearningAttemptEntity {
    @Id private UUID id;
    @Column(name = "content_item_id", nullable = false) private UUID contentItemId;
    @Enumerated(EnumType.STRING) @Column(name = "attempt_kind", nullable = false, length = 24) private LearningAttemptKind kind;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private LearningAttemptStatus status;
    @Column(name = "completed_units", nullable = false) private int completedUnits;
    @Column(name = "total_units", nullable = false) private int totalUnits;
    @Column(precision = 5, scale = 2) private BigDecimal score;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "result_json", nullable = false, columnDefinition = "jsonb") private Map<String, Object> result;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected LearningAttemptEntity() {}

    LearningAttemptEntity(UUID id, UUID contentItemId, LearningAttemptKind kind, LearningAttemptStatus status,
                          int completedUnits, int totalUnits, BigDecimal score, Map<String, Object> result,
                          Instant completedAt, Instant createdAt) {
        this.id = id; this.contentItemId = contentItemId; this.kind = kind; this.status = status;
        this.completedUnits = completedUnits; this.totalUnits = totalUnits; this.score = score;
        this.result = result; this.completedAt = completedAt; this.createdAt = createdAt;
    }

    UUID id() { return id; }
    UUID contentItemId() { return contentItemId; }
    LearningAttemptKind kind() { return kind; }
    LearningAttemptStatus status() { return status; }
    int completedUnits() { return completedUnits; }
    int totalUnits() { return totalUnits; }
    BigDecimal score() { return score; }
    Map<String, Object> result() { return result; }
    Instant completedAt() { return completedAt; }
    Instant createdAt() { return createdAt; }
}
