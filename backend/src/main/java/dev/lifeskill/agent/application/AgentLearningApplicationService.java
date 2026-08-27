package dev.lifeskill.agent.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.agent.application.port.AgentModelPort;
import dev.lifeskill.agent.application.port.AgentRunRepository;
import dev.lifeskill.agent.domain.Evidence;
import dev.lifeskill.learning.application.LearningApplicationService;
import dev.lifeskill.pulse.application.PulseApplicationService;

@Service
public class AgentLearningApplicationService {
    private final PulseApplicationService pulseService;
    private final AgentRunRepository repository;
    private final AgentModelPort model;
    private final LearningApplicationService learning;

    public AgentLearningApplicationService(
            PulseApplicationService pulseService,
            AgentRunRepository repository,
            AgentModelPort model,
            LearningApplicationService learning) {
        this.pulseService = pulseService;
        this.repository = repository;
        this.model = model;
        this.learning = learning;
    }

    @Transactional(readOnly = true)
    public List<Evidence> evidence(UUID pulseId) {
        var pulse = pulseService.get(pulseId);
        return repository.findEvidenceForClaim(pulse.primaryClaimId());
    }

    public LearningApplicationService.GeneratedLearningBundle generate(UUID pulseId) {
        var pulse = pulseService.get(pulseId);
        if (!"VERIFIED".equals(pulse.verificationStatus())) {
            throw new IllegalArgumentException("Only verified pulse items can generate learning content");
        }
        var existing = learning.findVerifiedBundle(pulse.skillRunId());
        if (existing.isPresent()) return existing.get();
        var claim = repository.findClaim(pulse.primaryClaimId())
                .orElseThrow(() -> new IllegalStateException("Verified pulse claim is missing"));
        List<Evidence> evidence = repository.findEvidenceForClaim(claim.id());
        if (evidence.isEmpty()) {
            // Learning is another publication surface, so it repeats the evidence guard instead of trusting UI state.
            throw new IllegalStateException("Verified learning content requires Evidence");
        }
        AgentModelPort.LearningResult draft = model.composeLearning(claim, evidence);
        return learning.createVerifiedBundle(
                pulse.skillRunId(), draft.folderName(), draft.folderDescription(),
                draft.pathTitle(), draft.pathBody(), draft.articleTitle(), draft.articleBody(),
                draft.quizTitle(), draft.quizBody());
    }
}
