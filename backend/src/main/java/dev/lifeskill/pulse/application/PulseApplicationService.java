package dev.lifeskill.pulse.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.pulse.application.port.PulseRepository;
import dev.lifeskill.pulse.domain.PulseItem;
import dev.lifeskill.shared.application.IdGenerator;
import java.time.Clock;

@Service
public class PulseApplicationService {
    private final PulseRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public PulseApplicationService(PulseRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PulseItem> list() {
        return repository.findPublished();
    }

    @Transactional(readOnly = true)
    public PulseItem get(UUID pulseId) {
        return repository.findById(pulseId)
                .orElseThrow(() -> new IllegalArgumentException("Pulse item was not found: " + pulseId));
    }

    @Transactional
    public PulseItem publishVerified(
            UUID runId,
            UUID claimId,
            String category,
            String title,
            String summary,
            int sourceCount,
            String recommendationReason) {
        return repository.save(new PulseItem(
                idGenerator.nextId(), runId, claimId, category, title, summary, "VERIFIED",
                sourceCount, recommendationReason, clock.instant(), null));
    }
}
