package dev.lifeskill.integration.deepseek;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.lifeskill.conversation.application.ModelProcessingException;
import dev.lifeskill.conversation.application.model.ModelDecision;
import dev.lifeskill.conversation.application.port.ModelPort;

@Component
@ConditionalOnProperty(name = "lifeskill.model.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableModelAdapter implements ModelPort {

    @Override
    public ModelDecision analyze(String userMessage) {
        throw new ModelProcessingException("Model integration is disabled");
    }
}
