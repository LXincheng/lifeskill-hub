package dev.lifeskill.conversation.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.lifeskill.conversation.domain.MessageRole;
import dev.lifeskill.conversation.domain.ProcessingStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "message")
class MessageEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "processing_steps", nullable = false, columnDefinition = "jsonb")
    private List<ProcessingStep> processingSteps = new ArrayList<>();

    @Column(name = "duration_ms")
    private Long durationMs;

    protected MessageEntity() {
    }

    MessageEntity(
            UUID id,
            MessageRole role,
            String content,
            Instant createdAt,
            List<ProcessingStep> processingSteps,
            Long durationMs) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.processingSteps = new ArrayList<>(processingSteps);
        this.durationMs = durationMs;
    }

    void attachTo(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    UUID id() {
        return id;
    }

    MessageRole role() {
        return role;
    }

    String content() {
        return content;
    }

    Instant createdAt() {
        return createdAt;
    }

    List<ProcessingStep> processingSteps() {
        return List.copyOf(processingSteps);
    }

    Long durationMs() {
        return durationMs;
    }
}
