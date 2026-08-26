package dev.lifeskill.pulse.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pulse_item")
class PulseItemEntity {
    @Id
    private UUID id;
    @Column(name = "skill_run_id")
    private UUID skillRunId;
    @Column(name = "content_item_id")
    private UUID contentItemId;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(nullable = false, length = 240)
    private String title;
    @Column(nullable = false)
    private String summary;
    @Column(name = "verification_status", nullable = false, length = 32)
    private String verificationStatus;
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;
    @Column(name = "read_at")
    private Instant readAt;

    protected PulseItemEntity() {}

    UUID id() { return id; }
    String category() { return category; }
    String title() { return title; }
    String summary() { return summary; }
    String verificationStatus() { return verificationStatus; }
    Instant publishedAt() { return publishedAt; }
    Instant readAt() { return readAt; }
}
