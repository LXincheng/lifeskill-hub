package dev.lifeskill.shared.api;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.lifeskill.conversation.application.ConversationNotFoundException;
import dev.lifeskill.agent.application.AgentRunNotFoundException;
import dev.lifeskill.learning.application.LearningResourceNotFoundException;
import dev.lifeskill.skill.application.IdempotencyConflictException;
import dev.lifeskill.skill.application.InvalidIdempotencyKeyException;
import dev.lifeskill.skill.application.InvalidSkillUpdateException;
import dev.lifeskill.skill.application.SkillDraftNotFoundException;
import dev.lifeskill.skill.application.SkillNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ConversationNotFoundException.class)
    ProblemDetail handleConversationNotFound(ConversationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://lifeskill.dev/problems/conversation-not-found"));
        problem.setTitle("Conversation not found");
        problem.setProperty("code", "CONVERSATION_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(AgentRunNotFoundException.class)
    ProblemDetail handleAgentRunNotFound(AgentRunNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), "Agent run not found", "AGENT_RUN_NOT_FOUND", "agent-run-not-found");
    }

    @ExceptionHandler(SkillDraftNotFoundException.class)
    ProblemDetail handleSkillDraftNotFound(SkillDraftNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "Skill draft not found",
                "SKILL_DRAFT_NOT_FOUND",
                "skill-draft-not-found");
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ProblemDetail handleInvalidIdempotencyKey(InvalidIdempotencyKeyException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                "Invalid idempotency key",
                "INVALID_IDEMPOTENCY_KEY",
                "invalid-idempotency-key");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        return problem(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "Idempotency conflict",
                "IDEMPOTENCY_CONFLICT",
                "idempotency-conflict");
    }

    @ExceptionHandler(SkillNotFoundException.class)
    ProblemDetail handleSkillNotFound(SkillNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), "Skill not found", "SKILL_NOT_FOUND", "skill-not-found");
    }

    @ExceptionHandler(LearningResourceNotFoundException.class)
    ProblemDetail handleLearningResourceNotFound(LearningResourceNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "Learning resource not found",
                "LEARNING_RESOURCE_NOT_FOUND",
                "learning-resource-not-found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidDomainInput(IllegalArgumentException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                "Invalid request",
                "INVALID_REQUEST",
                "invalid-request");
    }

    @ExceptionHandler(InvalidSkillUpdateException.class)
    ProblemDetail handleInvalidSkillUpdate(InvalidSkillUpdateException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), "Invalid skill update", "INVALID_SKILL_UPDATE", "invalid-skill-update");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Request validation failed");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://lifeskill.dev/problems/validation-error"));
        problem.setTitle("Invalid request");
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    private ProblemDetail problem(
            HttpStatus status,
            String detail,
            String title,
            String code,
            String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://lifeskill.dev/problems/" + type));
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }
}
