package dev.lifeskill.learning.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.learning.domain.LearningAnnotationKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_annotation")
class LearningAnnotationEntity {
    @Id private UUID id;
    @Column(name = "content_item_id", nullable = false) private UUID contentItemId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private LearningAnnotationKind kind;
    @Column(name = "selected_text", length = 2000) private String selectedText;
    @Column(length = 2000) private String note;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected LearningAnnotationEntity() {}

    LearningAnnotationEntity(UUID id, UUID contentItemId, LearningAnnotationKind kind,
                             String selectedText, String note, Instant createdAt) {
        this.id = id; this.contentItemId = contentItemId; this.kind = kind;
        this.selectedText = selectedText; this.note = note; this.createdAt = createdAt;
    }

    UUID id() { return id; }
    UUID contentItemId() { return contentItemId; }
    LearningAnnotationKind kind() { return kind; }
    String selectedText() { return selectedText; }
    String note() { return note; }
    Instant createdAt() { return createdAt; }
}
