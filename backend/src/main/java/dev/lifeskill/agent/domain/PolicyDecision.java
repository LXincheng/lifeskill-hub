package dev.lifeskill.agent.domain;

public record PolicyDecision(boolean allowed, String code, String reason) {
    public static PolicyDecision allow() {
        return new PolicyDecision(true, "PUBLISHED", "结论已通过证据关联、独立核验与官方来源检查。");
    }

    public static PolicyDecision block(String code, String reason) {
        return new PolicyDecision(false, code, reason);
    }
}
