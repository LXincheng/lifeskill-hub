package dev.lifeskill.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.lifeskill.conversation.application.port.ConversationRepository;
import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.shared.application.IdGenerator;

class ConversationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");
    private static final UUID CONVERSATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MESSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void createsAndUpdatesAnAggregateThroughTheRepositoryPort() {
        InMemoryConversationRepository repository = new InMemoryConversationRepository();
        Queue<UUID> ids = new ArrayDeque<>();
        ids.add(CONVERSATION_ID);
        ids.add(MESSAGE_ID);
        IdGenerator idGenerator = ids::remove;
        ConversationApplicationService service = new ConversationApplicationService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                idGenerator);

        Conversation created = service.createConversation();
        Conversation updated = service.sendUserMessage(created.id(), "学习 Spring AI 工具调用");

        assertThat(created.id()).isEqualTo(CONVERSATION_ID);
        assertThat(updated.messages()).singleElement().satisfies(message -> {
            assertThat(message.id()).isEqualTo(MESSAGE_ID);
            assertThat(message.createdAt()).isEqualTo(NOW);
        });
        assertThat(repository.findById(CONVERSATION_ID)).containsSame(updated);
    }

    private static final class InMemoryConversationRepository implements ConversationRepository {

        private final Map<UUID, Conversation> conversations = new HashMap<>();

        @Override
        public Conversation save(Conversation conversation) {
            conversations.put(conversation.id(), conversation);
            return conversation;
        }

        @Override
        public Optional<Conversation> findById(UUID conversationId) {
            return Optional.ofNullable(conversations.get(conversationId));
        }
    }
}
