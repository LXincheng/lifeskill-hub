package dev.lifeskill.agent.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.lifeskill.agent.application.AgentLearningApplicationService;
import dev.lifeskill.agent.application.AgentRunApplicationService;
import dev.lifeskill.agent.domain.Evidence;
import dev.lifeskill.learning.api.dto.ContentItemResponse;
import dev.lifeskill.learning.api.dto.LearningFolderResponse;

@RestController
@RequestMapping("/api")
public class AgentRunController {
    private final AgentRunApplicationService runs;
    private final AgentLearningApplicationService learning;
    private final SseAgentRunEventAdapter events;

    public AgentRunController(
            AgentRunApplicationService runs,
            AgentLearningApplicationService learning,
            SseAgentRunEventAdapter events) {
        this.runs = runs;
        this.learning = learning;
        this.events = events;
    }

    @PostMapping("/skills/{skillId}/runs")
    public ResponseEntity<AgentRunResponse> start(@PathVariable UUID skillId) {
        AgentRunResponse response = AgentRunResponse.from(runs.startManual(skillId));
        return ResponseEntity.accepted()
                .location(URI.create("/api/skill-runs/" + response.id()))
                .body(response);
    }

    @GetMapping("/skills/{skillId}/runs/latest")
    public AgentRunResponse latest(@PathVariable UUID skillId) {
        return AgentRunResponse.from(runs.latest(skillId));
    }

    @GetMapping("/skill-runs/{runId}")
    public AgentRunResponse get(@PathVariable UUID runId) {
        return AgentRunResponse.from(runs.get(runId));
    }

    @GetMapping(path = "/skill-runs/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID runId) {
        runs.get(runId);
        return events.subscribe(runId);
    }

    @GetMapping("/pulse-items/{pulseId}/evidence")
    public List<EvidenceResponse> evidence(@PathVariable UUID pulseId) {
        return learning.evidence(pulseId).stream().map(EvidenceResponse::from).toList();
    }

    @PostMapping("/pulse-items/{pulseId}/learning-folder")
    public ResponseEntity<LearningBundleResponse> generateLearning(@PathVariable UUID pulseId) {
        var bundle = learning.generate(pulseId);
        LearningBundleResponse response = new LearningBundleResponse(
                LearningFolderResponse.from(bundle.folder()),
                bundle.contentItems().stream().map(ContentItemResponse::from).toList());
        return ResponseEntity.created(URI.create("/api/learning-folders/" + bundle.folder().id())).body(response);
    }

    public record EvidenceResponse(
            UUID id, String sourceName, String sourceUrl, String title, String excerpt,
            Instant publishedAt, Instant fetchedAt, String contentHash, boolean officialSource) {
        static EvidenceResponse from(Evidence evidence) {
            return new EvidenceResponse(
                    evidence.id(), evidence.sourceName(), evidence.sourceUrl(), evidence.title(), evidence.excerpt(),
                    evidence.publishedAt(), evidence.fetchedAt(), evidence.contentHash(), evidence.officialSource());
        }
    }

    public record LearningBundleResponse(
            LearningFolderResponse folder,
            List<ContentItemResponse> contentItems) {}
}
