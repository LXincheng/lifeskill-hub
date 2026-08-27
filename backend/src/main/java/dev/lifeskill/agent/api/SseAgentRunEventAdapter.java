package dev.lifeskill.agent.api;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.lifeskill.agent.application.port.AgentRunEventPort;

@Component
class SseAgentRunEventAdapter implements AgentRunEventPort {
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    SseEmitter subscribe(UUID runId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        emitters.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(runId, emitter));
        emitter.onTimeout(() -> remove(runId, emitter));
        emitter.onError(ignored -> remove(runId, emitter));
        send(emitter, "snapshot", runId);
        return emitter;
    }

    @Override
    public void changed(UUID runId) {
        broadcast(runId, "changed", false);
    }

    @Override
    public void finished(UUID runId) {
        broadcast(runId, "finished", true);
    }

    private void broadcast(UUID runId, String eventName, boolean complete) {
        for (SseEmitter emitter : emitters.getOrDefault(runId, new CopyOnWriteArrayList<>())) {
            if (send(emitter, eventName, runId) && complete) emitter.complete();
        }
        if (complete) emitters.remove(runId);
    }

    private boolean send(SseEmitter emitter, String eventName, UUID runId) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(Map.of("runId", runId)));
            return true;
        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
            return false;
        }
    }

    private void remove(UUID runId, SseEmitter emitter) {
        var current = emitters.get(runId);
        if (current == null) return;
        current.remove(emitter);
        if (current.isEmpty()) emitters.remove(runId);
    }
}
