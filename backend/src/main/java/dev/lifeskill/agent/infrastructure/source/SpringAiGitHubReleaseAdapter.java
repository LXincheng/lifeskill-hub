package dev.lifeskill.agent.infrastructure.source;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import dev.lifeskill.agent.application.port.OfficialSourcePort;

@Component
public class SpringAiGitHubReleaseAdapter implements OfficialSourcePort {
    static final URI RELEASES_URI = URI.create("https://api.github.com/repos/spring-projects/spring-ai/releases?per_page=3");

    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public SpringAiGitHubReleaseAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<OfficialSourceDocument> collect(Instant fetchedAt) {
        try {
            HttpRequest request = HttpRequest.newBuilder(RELEASES_URI)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "LifeSkill-Hub/1.0")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("GitHub official source returned HTTP " + response.statusCode());
            }
            JsonNode releases = objectMapper.readTree(response.body());
            if (!releases.isArray()) throw new IllegalStateException("GitHub releases response is not an array");
            return java.util.stream.StreamSupport.stream(releases.spliterator(), false)
                    .filter(item -> !item.path("draft").asBoolean() && !item.path("prerelease").asBoolean())
                    .map(item -> toDocument(item, fetchedAt))
                    .toList();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Official source collection was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to collect Spring AI official releases", exception);
        }
    }

    private OfficialSourceDocument toDocument(JsonNode item, Instant fetchedAt) {
        String url = requiredText(item, "html_url");
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"github.com".equalsIgnoreCase(uri.getHost())
                || !uri.getPath().startsWith("/spring-projects/spring-ai/releases/")) {
            throw new IllegalStateException("GitHub returned a release URL outside the official repository");
        }
        String tag = requiredText(item, "tag_name");
        String title = item.path("name").asText(tag).trim();
        String body = item.path("body").asText("").trim();
        Instant publishedAt = Instant.parse(requiredText(item, "published_at"));
        String canonical = tag + "\n" + title + "\n" + publishedAt + "\n" + body;
        return new OfficialSourceDocument(
                "GITHUB_RELEASE",
                "Spring AI GitHub Releases",
                url,
                tag,
                title,
                abbreviate(body.isBlank() ? "Spring AI 官方发布 " + tag : body, 1200),
                abbreviate(body, 20_000),
                publishedAt,
                fetchedAt,
                sha256(canonical),
                true);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isBlank()) throw new IllegalStateException("GitHub release is missing " + field);
        return value;
    }

    private String abbreviate(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
