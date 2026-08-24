package dev.lifeskill.shared.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class SystemStatusController {

    @GetMapping
    public Map<String, Object> status() {
        return Map.of(
                "service", "lifeskill-backend",
                "status", "ready",
                "timestamp", Instant.now());
    }
}
