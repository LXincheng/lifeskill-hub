package dev.lifeskill.conversation.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.conversation.application.port.ConversationRepository;
import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.shared.application.IdGenerator;

@Service
public class ConversationApplicationService {

    private final ConversationRepository conversationRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ConversationApplicationService(
            ConversationRepository conversationRepository,
            Clock clock,
            IdGenerator idGenerator) {
        this.conversationRepository = conversationRepository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Conversation createConversation() {
        Conversation conversation = Conversation.start(idGenerator.nextId(), clock.instant());
        return conversationRepository.save(conversation);
    }

    @Transactional
    public Conversation sendUserMessage(UUID conversationId, String content) {
        Conversation conversation = getRequiredConversation(conversationId);
        conversation.addUserMessage(idGenerator.nextId(), content, clock.instant());
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(UUID conversationId) {
        return getRequiredConversation(conversationId);
    }

    private Conversation getRequiredConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }
}
