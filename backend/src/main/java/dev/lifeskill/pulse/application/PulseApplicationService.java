package dev.lifeskill.pulse.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.pulse.application.port.PulseRepository;
import dev.lifeskill.pulse.domain.PulseItem;

@Service
public class PulseApplicationService {
    private final PulseRepository repository;

    public PulseApplicationService(PulseRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PulseItem> list() {
        return repository.findPublished();
    }
}
