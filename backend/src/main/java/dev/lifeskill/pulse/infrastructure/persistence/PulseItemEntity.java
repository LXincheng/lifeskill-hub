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
    @Column(name = "primary_claim_id", nullable = false)
    private UUID primaryClaimId;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(nullable = false, length = 240)
    private String title;
    @Column(nullable = false)
    private String summary;
    @Column(name = "verification_status", nullable = false, length = 32)
    private String verificationStatus;
    @Column(name = "source_count", nullable = false)
    private int sourceCount;
    @Column(name = "recommendation_reason", nullable = false)
    private String recommendationReason;
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;
    @Column(name = "read_at")
    private Instant readAt;

    protected PulseItemEntity() {}

    PulseItemEntity(
            UUID id, UUID skillRunId, UUID primaryClaimId, String category, String title, String summary,
            String verificationStatus, int sourceCount, String recommendationReason, Instant publishedAt, Instant readAt) {
        this.id = id;
        this.skillRunId = skillRunId;
        this.primaryClaimId = primaryClaimId;
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.verificationStatus = verificationStatus;
        this.sourceCount = sourceCount;
        this.recommendationReason = recommendationReason;
        this.publishedAt = publishedAt;
        this.readAt = readAt;
    }

    UUID id() { return id; }
    UUID skillRunId() { return skillRunId; }
    UUID primaryClaimId() { return primaryClaimId; }
    String category() { return category; }
    String title() { return title; }
    String summary() { return summary; }
    String verificationStatus() { return verificationStatus; }
    int sourceCount() { return sourceCount; }
    String recommendationReason() { return recommendationReason; }
    Instant publishedAt() { return publishedAt; }
    Instant readAt() { return readAt; }
}
