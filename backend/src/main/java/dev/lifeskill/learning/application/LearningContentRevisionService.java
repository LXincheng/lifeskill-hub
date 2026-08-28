package dev.lifeskill.learning.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.lifeskill.agent.application.port.AgentModelPort;
import dev.lifeskill.learning.domain.ContentItem;
import dev.lifeskill.learning.domain.ContentItemType;
import dev.lifeskill.learning.domain.LearningAnnotation;
import dev.lifeskill.learning.domain.LearningAnnotationKind;

@Service
public class LearningContentRevisionService {
    private final LearningApplicationService learning;
    private final AgentModelPort model;

    public LearningContentRevisionService(LearningApplicationService learning, AgentModelPort model) {
        this.learning = learning;
        this.model = model;
    }

    public ContentItem regenerate(UUID contentId) {
        ContentItem content = learning.getContent(contentId);
        if (!"AI_GENERATED".equals(content.verificationStatus())) {
            throw new IllegalArgumentException("Only AI-generated learning content can be regenerated");
        }
        List<String> feedback = learning.listAnnotations(contentId).stream()
                .filter(annotation -> annotation.kind() == LearningAnnotationKind.FEEDBACK)
                .map(LearningAnnotation::note).toList();
        if (feedback.isEmpty()) throw new IllegalArgumentException("Add at least one feedback note before regeneration");
        AgentModelPort.ContentRevision revision = model.reviseLearningContent(
                content.type().name(), content.title(), content.body(), String.join("\n- ", feedback));
        validateStructure(content.type(), revision.body());
        return learning.applyRegeneration(contentId, revision.title(), revision.body());
    }

    private void validateStructure(ContentItemType type, String body) {
        if (body == null || body.isBlank()) throw new IllegalStateException("Regenerated content is empty");
        if (type == ContentItemType.LEARNING_PATH) {
            // Markdown writers commonly prefix task boxes with bullets; both forms
            // represent the same safe structure and should survive deterministic validation.
            long units = body.lines().map(String::trim)
                    .filter(line -> line.matches("^(?:[-*]|\\d+[.)])?\\s*\\[ \\]\\s+.+$"))
                    .count();
            if (units < 3 || units > 10) throw new IllegalArgumentException(
                    "Agent returned a learning path with an invalid step count; the original content was kept");
        }
        if (type == ContentItemType.QUIZ) {
            long answers = java.util.regex.Pattern.compile("(?m)^(?:答案|answer)\\s*[:：]\\s*[12]\\s*$",
                    java.util.regex.Pattern.CASE_INSENSITIVE).matcher(body).results().count();
            if (answers < 3 || answers > 8) throw new IllegalArgumentException(
                    "Agent returned an invalid quiz structure; the original content was kept");
        }
    }
}
