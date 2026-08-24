package dev.lifeskill.conversation.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "conversation")
class ConversationEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC, id ASC")
    private List<MessageEntity> messages = new ArrayList<>();

    protected ConversationEntity() {
    }

    ConversationEntity(UUID id, String title, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void update(String title, Instant updatedAt) {
        this.title = title;
        this.updatedAt = updatedAt;
    }

    void addMessage(MessageEntity message) {
        message.attachTo(this);
        messages.add(message);
    }

    UUID id() {
        return id;
    }

    String title() {
        return title;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    List<MessageEntity> messages() {
        return messages;
    }
}
