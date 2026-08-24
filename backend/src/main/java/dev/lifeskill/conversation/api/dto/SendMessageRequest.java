package dev.lifeskill.conversation.api.dto;

import dev.lifeskill.conversation.domain.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = Message.MAX_CONTENT_LENGTH) String content) {
}
