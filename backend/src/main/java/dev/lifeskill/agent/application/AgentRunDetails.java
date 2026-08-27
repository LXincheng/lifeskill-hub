package dev.lifeskill.agent.application;

import java.util.List;

import dev.lifeskill.agent.domain.AgentRun;
import dev.lifeskill.agent.domain.AgentStep;

public record AgentRunDetails(AgentRun run, List<AgentStep> steps) {
    public AgentRunDetails {
        steps = List.copyOf(steps);
    }
}
