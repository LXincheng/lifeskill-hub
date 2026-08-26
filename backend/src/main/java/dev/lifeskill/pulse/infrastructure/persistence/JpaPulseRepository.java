package dev.lifeskill.pulse.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import dev.lifeskill.pulse.application.port.PulseRepository;
import dev.lifeskill.pulse.domain.PulseItem;

@Repository
class JpaPulseRepository implements PulseRepository {
    private final SpringDataPulseRepository repository;

    JpaPulseRepository(SpringDataPulseRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PulseItem> findPublished() {
        return repository.findAllByOrderByPublishedAtDesc().stream()
                .map(entity -> new PulseItem(
                        entity.id(), entity.category(), entity.title(), entity.summary(),
                        entity.verificationStatus(), entity.publishedAt(), entity.readAt()))
                .toList();
    }
}
