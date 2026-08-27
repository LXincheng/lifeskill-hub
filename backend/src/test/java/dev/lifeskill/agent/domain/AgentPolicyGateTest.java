package dev.lifeskill.agent.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AgentPolicyGateTest {
    private final AgentPolicyGate gate = new AgentPolicyGate();

    @Test
    void blocksClaimWithoutEvidenceBeforeAnyPublicationSurface() {
        Claim claim = new Claim(
                UUID.randomUUID(), UUID.randomUUID(), "Spring AI 发布了新版本。", List.of(),
                "VERIFIED", 0.99, "模型声称已核验，但没有证据关系。", Instant.now(), Instant.now());

        PolicyDecision decision = gate.evaluate(claim, List.of());

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("CLAIM_WITHOUT_EVIDENCE");
    }

    @Test
    void allowsOnlyVerifiedClaimCitingTheOfficialSpringAiRepository() {
        UUID runId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        Evidence evidence = new Evidence(
                evidenceId, runId, "GITHUB_RELEASE", "Spring AI GitHub Releases",
                "https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0",
                "v2.0.0", "Spring AI 2.0.0", "official notes", "official notes", Instant.now(),
                Instant.now(), "a".repeat(64), true);
        Claim claim = new Claim(
                UUID.randomUUID(), runId, "Spring AI 发布了 2.0.0。", List.of(evidenceId),
                "VERIFIED", 0.91, "官方 Release 直接支持。", Instant.now(), Instant.now());

        assertThat(gate.evaluate(claim, List.of(evidence)).allowed()).isTrue();
    }

    @Test
    void blocksUnverifiedOrNonOfficialEvidenceEvenWhenAnIdExists() {
        UUID runId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        Evidence evidence = new Evidence(
                evidenceId, runId, "WEB", "Unknown blog", "https://example.com/spring-ai",
                "post", "Unknown", "excerpt", "body", Instant.now(), Instant.now(), "b".repeat(64), false);
        Claim claim = new Claim(
                UUID.randomUUID(), runId, "未经核验的结论", List.of(evidenceId),
                "VERIFIED", 0.99, "", Instant.now(), Instant.now());

        assertThat(gate.evaluate(claim, List.of(evidence)).code()).isEqualTo("OFFICIAL_SOURCE_REQUIRED");
    }
}
