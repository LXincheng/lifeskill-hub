package dev.lifeskill.learning.application;

import java.util.UUID;

public class LearningResourceNotFoundException extends RuntimeException {
    public LearningResourceNotFoundException(String resource, UUID id) {
        super(resource + " " + id + " was not found");
    }
}
