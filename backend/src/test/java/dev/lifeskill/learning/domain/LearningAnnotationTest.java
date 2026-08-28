package dev.lifeskill.learning.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningAnnotationTest {
    @Test
    void requiresContentForTheSelectedAnnotationKind() {
        assertThatThrownBy(() -> new LearningAnnotation(
                UUID.randomUUID(), UUID.randomUUID(), LearningAnnotationKind.HIGHLIGHT,
                "  ", null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LearningAnnotation(
                UUID.randomUUID(), UUID.randomUUID(), LearningAnnotationKind.FEEDBACK,
                null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
