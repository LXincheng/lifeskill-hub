package dev.lifeskill.conversation.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.lifeskill.conversation.api.dto.ConversationResponse;
import dev.lifeskill.conversation.api.dto.SendMessageRequest;
import dev.lifeskill.conversation.application.ConversationApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationApplicationService conversationService;

    public ConversationController(ConversationApplicationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation() {
        ConversationResponse response = ConversationResponse.from(conversationService.createConversation());
        return ResponseEntity.created(URI.create("/api/conversations/" + response.id())).body(response);
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getConversation(@PathVariable UUID conversationId) {
        return ConversationResponse.from(conversationService.getConversation(conversationId));
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ConversationResponse.from(conversationService.sendUserMessage(conversationId, request.content()));
    }

}
