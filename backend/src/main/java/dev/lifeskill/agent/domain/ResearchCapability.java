package dev.lifeskill.agent.domain;

import java.util.Locale;

public enum ResearchCapability {
    JAVA_OFFICIAL,
    GOLD_MARKET,
    LEARNING_PLAN,
    TICKET_ASSIST,
    UNSUPPORTED;

    public static ResearchCapability detect(String objective) {
        String normalized = objective == null ? "" : objective.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "学习计划", "学习路径", "课程计划", "学习方案", "study plan", "learning path")) return LEARNING_PLAN;
        if (containsAny(normalized, "黄金", "金价", "贵金属", "gold", "lbma")) return GOLD_MARKET;
        if (containsAny(normalized, "电影票", "影院", "影城", "抢票", "锁座", "皇帝座", "imax")) return TICKET_ASSIST;
        if (containsAny(normalized, "spring ai", "java agent", "langchain4j")) return JAVA_OFFICIAL;
        return UNSUPPORTED;
    }

    public boolean isRunnableResearch() {
        return this == GOLD_MARKET || this == JAVA_OFFICIAL;
    }

    public boolean isLearningPlan() {
        return this == LEARNING_PLAN;
    }

    public static boolean requestsRecurringExecution(String objective) {
        String normalized = objective == null ? "" : objective.toLowerCase(Locale.ROOT);
        return containsAny(normalized, "持续", "长期", "定期", "每周", "每天", "每日", "盯着", "监控", "关注更新");
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }
}
