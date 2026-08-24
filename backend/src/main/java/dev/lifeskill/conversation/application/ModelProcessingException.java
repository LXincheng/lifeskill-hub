package dev.lifeskill.conversation.application;

public class ModelProcessingException extends RuntimeException {

    public ModelProcessingException(String message) {
        super(message);
    }

    public ModelProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
