package dev.lifeskill.agent.application.port;

import java.util.List;

import dev.lifeskill.agent.domain.Claim;
import dev.lifeskill.agent.domain.Evidence;

public interface AgentModelPort {
    ResearchResult research(String objective, List<Evidence> evidence);

    VerificationResult verify(String objective, Claim claim, List<Evidence> evidence);

    CompositionResult compose(String objective, Claim claim, List<Evidence> evidence);

    ReportResult composeReport(String objective, Claim claim, List<Evidence> evidence);

    LearningResult composeLearning(Claim claim, List<Evidence> evidence);

    LearningResult composePersonalLearning(String objective);

    ContentRevision reviseLearningContent(String type, String title, String body, String feedback);

    record ResearchResult(String statement, List<String> evidenceIds) {
    }

    record VerificationResult(boolean verified, double confidence, String summary, List<String> evidenceIds) {
    }

    record CompositionResult(String title, String summary, String category, String recommendationReason) {
    }

    record ReportResult(String title, String body) {
    }

    record LearningResult(
            String folderName,
            String folderDescription,
            String pathTitle,
            String pathBody,
            String articleTitle,
            String articleBody,
            String quizTitle,
            String quizBody) {
    }

    record ContentRevision(String title, String body) {
    }
}
