package dev.lifeskill.conversation.application.port;

import dev.lifeskill.conversation.application.model.ModelDecision;

public interface ModelPort {

    ModelDecision analyze(String userMessage);
}
