package dev.lifeskill.pulse.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                        entity.id(), entity.skillRunId(), entity.primaryClaimId(), entity.category(), entity.title(), entity.summary(),
                        entity.verificationStatus(), entity.sourceCount(), entity.recommendationReason(), entity.publishedAt(), entity.readAt()))
                .toList();
    }

    @Override
    public Optional<PulseItem> findById(UUID pulseId) {
        return repository.findById(pulseId).map(this::toDomain);
    }

    @Override
    public PulseItem save(PulseItem item) {
        return toDomain(repository.save(new PulseItemEntity(
                item.id(), item.skillRunId(), item.primaryClaimId(), item.category(), item.title(), item.summary(),
                item.verificationStatus(), item.sourceCount(), item.recommendationReason(), item.publishedAt(), item.readAt())));
    }

    private PulseItem toDomain(PulseItemEntity entity) {
        return new PulseItem(
                entity.id(), entity.skillRunId(), entity.primaryClaimId(), entity.category(), entity.title(), entity.summary(),
                entity.verificationStatus(), entity.sourceCount(), entity.recommendationReason(), entity.publishedAt(), entity.readAt());
    }
}
