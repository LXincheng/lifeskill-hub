package dev.lifeskill.learning.api.dto;

import dev.lifeskill.learning.domain.ContentItemType;
import jakarta.validation.constraints.Size;

public record ContentItemRequest(
        ContentItemType type,
        @Size(min = 1, max = 240) String title,
        @Size(min = 1, max = 20_000) String body) {
}
