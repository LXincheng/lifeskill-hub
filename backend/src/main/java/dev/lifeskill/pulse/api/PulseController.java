package dev.lifeskill.pulse.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lifeskill.pulse.application.PulseApplicationService;
import dev.lifeskill.pulse.domain.PulseItem;

@RestController
@RequestMapping("/api/pulse-items")
public class PulseController {
    private final PulseApplicationService service;

    public PulseController(PulseApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<PulseItemResponse> list() {
        return service.list().stream().map(PulseItemResponse::from).toList();
    }

    public record PulseItemResponse(
            UUID id,
            UUID skillRunId,
            String category,
            String title,
            String summary,
            String verificationStatus,
            int sourceCount,
            String recommendationReason,
            Instant publishedAt,
            Instant readAt) {
        static PulseItemResponse from(PulseItem item) {
            return new PulseItemResponse(
                    item.id(), item.skillRunId(), item.category(), item.title(), item.summary(),
                    item.verificationStatus(), item.sourceCount(), item.recommendationReason(), item.publishedAt(), item.readAt());
        }
    }
}
