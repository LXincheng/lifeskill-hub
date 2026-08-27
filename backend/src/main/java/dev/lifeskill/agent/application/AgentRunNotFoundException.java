package dev.lifeskill.agent.application;

import java.util.UUID;

public class AgentRunNotFoundException extends RuntimeException {
    public AgentRunNotFoundException(UUID runId) {
        super("Agent run was not found: " + runId);
    }
}
