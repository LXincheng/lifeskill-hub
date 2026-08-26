package dev.lifeskill.skill.application;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must contain between 1 and 120 characters");
    }
}
