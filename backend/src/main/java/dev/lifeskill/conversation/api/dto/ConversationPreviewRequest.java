package dev.lifeskill.conversation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationPreviewRequest(
        @NotBlank @Size(max = 4_000) String message) {
}
