package dev.lifeskill.agent.application.port;

import java.util.UUID;

public interface AgentRunEventPort {
    void changed(UUID runId);

    void finished(UUID runId);
}
