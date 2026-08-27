package dev.lifeskill.agent.application.port;

import java.time.Instant;
import java.util.List;

public interface OfficialSourcePort {
    List<OfficialSourceDocument> collect(Instant fetchedAt);

    record OfficialSourceDocument(
            String sourceType,
            String sourceName,
            String sourceUrl,
            String externalId,
            String title,
            String excerpt,
            String rawContent,
            Instant publishedAt,
            Instant fetchedAt,
            String contentHash,
            boolean officialSource) {
    }
}
