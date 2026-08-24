package dev.lifeskill.conversation.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.lifeskill.conversation.api.dto.ConversationPreviewRequest;
import dev.lifeskill.conversation.api.dto.ConversationPreviewResponse;
import dev.lifeskill.conversation.application.ConversationPreviewService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationPreviewService previewService;

    public ConversationController(ConversationPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/preview")
    @ResponseStatus(HttpStatus.OK)
    public ConversationPreviewResponse preview(
            @Valid @RequestBody ConversationPreviewRequest request) {
        return previewService.preview(request.message());
    }
}
