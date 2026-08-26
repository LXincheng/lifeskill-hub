package dev.lifeskill.learning.api.dto;

import jakarta.validation.constraints.Size;

public record LearningFolderRequest(
        @Size(min = 1, max = 160) String name,
        @Size(max = 1000) String description) {
}
