package dev.lifeskill.integration.deepseek;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.lifeskill.agent.application.port.AgentModelPort;
import dev.lifeskill.agent.domain.Claim;
import dev.lifeskill.agent.domain.Evidence;

@Component
@ConditionalOnProperty(name = "lifeskill.model.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableAgentModelAdapter implements AgentModelPort {
    private IllegalStateException disabled() {
        return new IllegalStateException("Agent model integration is disabled");
    }

    @Override
    public ResearchResult research(String objective, List<Evidence> evidence) { throw disabled(); }

    @Override
    public VerificationResult verify(String objective, Claim claim, List<Evidence> evidence) { throw disabled(); }

    @Override
    public CompositionResult compose(String objective, Claim claim, List<Evidence> evidence) { throw disabled(); }

    @Override
    public ReportResult composeReport(String objective, Claim claim, List<Evidence> evidence) { throw disabled(); }

    @Override
    public LearningResult composeLearning(Claim claim, List<Evidence> evidence) { throw disabled(); }
}
