package dev.lifeskill.skill.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import dev.lifeskill.skill.domain.SkillDraftStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill_draft")
class SkillDraftEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "source_message_id", nullable = false, unique = true)
    private UUID sourceMessageId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_day_of_week", nullable = false, length = 12)
    private DayOfWeek scheduleDayOfWeek;

    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;

    @Column(nullable = false, length = 80)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SkillDraftStatus status;

    @Column(name = "prompt_version", nullable = false, length = 40)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillDraftEntity() {
    }

    SkillDraftEntity(
            UUID id,
            UUID conversationId,
            UUID sourceMessageId,
            String title,
            String objective,
            DayOfWeek scheduleDayOfWeek,
            LocalTime scheduleTime,
            String timezone,
            SkillDraftStatus status,
            String promptVersion,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.sourceMessageId = sourceMessageId;
        this.title = title;
        this.objective = objective;
        this.scheduleDayOfWeek = scheduleDayOfWeek;
        this.scheduleTime = scheduleTime;
        this.timezone = timezone;
        this.status = status;
        this.promptVersion = promptVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID id() { return id; }
    UUID conversationId() { return conversationId; }
    UUID sourceMessageId() { return sourceMessageId; }
    String title() { return title; }
    String objective() { return objective; }
    DayOfWeek scheduleDayOfWeek() { return scheduleDayOfWeek; }
    LocalTime scheduleTime() { return scheduleTime; }
    String timezone() { return timezone; }
    SkillDraftStatus status() { return status; }
    String promptVersion() { return promptVersion; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
