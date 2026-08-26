package dev.lifeskill.pulse.application.port;

import java.util.List;

import dev.lifeskill.pulse.domain.PulseItem;

public interface PulseRepository {
    List<PulseItem> findPublished();
}
