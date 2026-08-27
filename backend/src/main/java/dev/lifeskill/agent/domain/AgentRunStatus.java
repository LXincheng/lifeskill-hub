package dev.lifeskill.agent.domain;

import java.util.EnumSet;

public enum AgentRunStatus {
    RECEIVED,
    PLANNING,
    COLLECTING,
    RESEARCHING,
    VERIFYING,
    COMPOSING,
    POLICY_CHECK,
    COMPLETED,
    BLOCKED,
    FAILED,
    TIMED_OUT;

    public boolean isTerminal() {
        return EnumSet.of(COMPLETED, BLOCKED, FAILED, TIMED_OUT).contains(this);
    }
}
