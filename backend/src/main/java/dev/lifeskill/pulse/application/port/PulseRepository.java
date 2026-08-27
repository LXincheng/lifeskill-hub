package dev.lifeskill.pulse.application.port;

import java.util.List;

import dev.lifeskill.pulse.domain.PulseItem;

public interface PulseRepository {
    List<PulseItem> findPublished();

    java.util.Optional<PulseItem> findById(java.util.UUID pulseId);

    PulseItem save(PulseItem item);
}
