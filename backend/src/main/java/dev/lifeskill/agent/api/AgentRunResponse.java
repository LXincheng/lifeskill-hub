package dev.lifeskill.agent.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.lifeskill.agent.application.AgentRunDetails;
import dev.lifeskill.agent.domain.AgentStep;

public record AgentRunResponse(
        UUID id,
        UUID skillId,
        int skillVersion,
        UUID conversationId,
        String capability,
        UUID resultContentId,
        UUID auditId,
        String triggerType,
        String status,
        int maxSteps,
        int stepCount,
        Instant startedAt,
        Instant timeoutAt,
        Instant completedAt,
        Long durationMs,
        String failureSummary,
        List<StepResponse> steps) {

    static AgentRunResponse from(AgentRunDetails details) {
        var run = details.run();
        return new AgentRunResponse(
                run.id(), run.skillId(), run.skillVersion(), run.conversationId(), run.capability(), run.resultContentId(),
                run.auditId(), run.triggerType(), run.status().name(),
                run.maxSteps(), run.stepCount(), run.startedAt(), run.timeoutAt(), run.completedAt(), run.durationMs(),
                run.failureSummary(), details.steps().stream().map(StepResponse::from).toList());
    }

    public record StepResponse(
            int order,
            String role,
            String status,
            String eventType,
            String inputSummary,
            String outputSummary,
            String toolName,
            String sourceUrl,
            Long durationMs,
            String errorSummary,
            Instant completedAt) {
        static StepResponse from(AgentStep step) {
            return new StepResponse(
                    step.order(), step.role(), step.status(), step.eventType(), step.inputSummary(), step.outputSummary(),
                    step.toolName(), step.sourceUrl(), step.durationMs(), step.errorSummary(), step.completedAt());
        }
    }
}
