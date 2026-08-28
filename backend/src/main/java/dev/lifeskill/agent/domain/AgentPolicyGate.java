package dev.lifeskill.agent.domain;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class AgentPolicyGate {

    public PolicyDecision evaluate(Claim claim, List<Evidence> evidence) {
        if (claim.evidenceIds().isEmpty()) {
            return PolicyDecision.block("CLAIM_WITHOUT_EVIDENCE", "Claim 没有关联 Evidence，禁止发布。");
        }
        Set<java.util.UUID> available = evidence.stream().map(Evidence::id).collect(Collectors.toSet());
        if (!available.containsAll(claim.evidenceIds())) {
            return PolicyDecision.block("EVIDENCE_REFERENCE_INVALID", "Claim 引用了本次运行不存在的 Evidence。");
        }
        List<Evidence> cited = evidence.stream().filter(item -> claim.evidenceIds().contains(item.id())).toList();
        if (cited.stream().noneMatch(Evidence::officialSource)) {
            return PolicyDecision.block("OFFICIAL_SOURCE_REQUIRED", "本次结论缺少官方一手来源。");
        }
        if (cited.stream().anyMatch(item -> !isAllowedOfficialUrl(item.sourceUrl()))) {
            return PolicyDecision.block("SOURCE_URL_NOT_ALLOWED", "Evidence URL 不在允许的官方来源边界内。");
        }
        if (!"VERIFIED".equals(claim.verificationStatus()) || claim.confidence() < 0.75) {
            return PolicyDecision.block("VERIFICATION_FAILED", "Verifier 未达到发布所需的核验状态或置信阈值。");
        }
        return PolicyDecision.allow();
    }

    private boolean isAllowedOfficialUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            boolean springAi = "github.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath().startsWith("/spring-projects/spring-ai/");
            boolean goldhub = "www.gold.org".equalsIgnoreCase(uri.getHost())
                    && uri.getPath().startsWith("/goldhub/");
            return springAi || goldhub;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
