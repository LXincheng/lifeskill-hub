package dev.lifeskill.skill.application;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key has already been used for another skill draft");
    }
}
