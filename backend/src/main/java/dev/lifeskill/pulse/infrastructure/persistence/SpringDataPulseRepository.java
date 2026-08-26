package dev.lifeskill.pulse.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPulseRepository extends JpaRepository<PulseItemEntity, UUID> {
    List<PulseItemEntity> findAllByOrderByPublishedAtDesc();
}
