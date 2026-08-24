package dev.lifeskill.conversation.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.conversation.application.port.ConversationRepository;
import dev.lifeskill.conversation.domain.Conversation;
import dev.lifeskill.shared.application.IdGenerator;
import dev.lifeskill.skill.application.port.SkillDraftRepository;
import dev.lifeskill.skill.domain.SkillDraft;

@Service
public class ConversationCompletionApplicationService {

    private final ConversationRepository conversationRepository;
    private final SkillDraftRepository skillDraftRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ConversationCompletionApplicationService(
            ConversationRepository conversationRepository,
            SkillDraftRepository skillDraftRepository,
            Clock clock,
            IdGenerator idGenerator) {
        this.conversationRepository = conversationRepository;
        this.skillDraftRepository = skillDraftRepository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Conversation complete(UUID conversationId, String assistantContent, Optional<SkillDraft> draft) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.addAssistantMessage(idGenerator.nextId(), assistantContent, clock.instant());
        Conversation savedConversation = conversationRepository.save(conversation);
        draft.ifPresent(skillDraftRepository::save);
        return savedConversation;
    }
}
