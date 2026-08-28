package dev.lifeskill.agent.infrastructure.source;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import dev.lifeskill.agent.application.port.OfficialSourcePort;

@Component
public class WorldGoldCouncilResearchAdapter implements OfficialSourcePort {
    static final URI GOLDHUB_URI = URI.create("https://www.gold.org/goldhub");
    private static final DateTimeFormatter PUBLISHED_FORMAT = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String capability() {
        return "GOLD_MARKET";
    }

    @Override
    public List<OfficialSourceDocument> collect(Instant fetchedAt) {
        Document hub = fetch(GOLDHUB_URI);
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        for (Element link : hub.select("a[href]")) {
            String url = link.absUrl("href");
            if (isResearchPage(url)) urls.add(url);
            if (urls.size() == 4) break;
        }
        if (urls.isEmpty()) throw new IllegalStateException("World Gold Council did not expose current research links");
        return urls.stream().map(url -> toDocument(URI.create(url), fetchedAt)).toList();
    }

    private OfficialSourceDocument toDocument(URI uri, Instant fetchedAt) {
        Document page = fetch(uri);
        String title = text(page.selectFirst("h1"), "World Gold Council research title");
        Element main = page.selectFirst("main");
        String raw = (main == null ? page.body() : main).text().replaceAll("\\s+", " ").trim();
        if (raw.length() < 120) throw new IllegalStateException("World Gold Council research body is unexpectedly empty");
        Instant publishedAt = publishedAt(page, raw);
        String canonical = uri + "\n" + title + "\n" + publishedAt + "\n" + raw;
        return new OfficialSourceDocument(
                "OFFICIAL_RESEARCH",
                "World Gold Council · Goldhub",
                uri.toString(),
                uri.getPath(),
                title,
                abbreviate(raw, 1_600),
                abbreviate(raw, 20_000),
                publishedAt,
                fetchedAt,
                sha256(canonical),
                true);
    }

    private Document fetch(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("User-Agent", "LifeSkill-Hub/1.0 (+official research adapter)")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("World Gold Council returned HTTP " + response.statusCode());
            return Jsoup.parse(response.body(), uri.toString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("World Gold Council collection was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to collect World Gold Council official research", exception);
        }
    }

    private boolean isResearchPage(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"www.gold.org".equalsIgnoreCase(uri.getHost())) return false;
            String path = uri.getPath();
            return path.startsWith("/goldhub/research/")
                    && !path.equals("/goldhub/research/library")
                    && !path.contains("/market-commentary-and-outlook")
                    && !path.contains("/market-update");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Instant publishedAt(Document page, String raw) {
        String metadata = page.select("meta[property=article:published_time],meta[name=date],time[datetime]").stream()
                .map(element -> element.hasAttr("content") ? element.attr("content") : element.attr("datetime"))
                .filter(value -> !value.isBlank()).findFirst().orElse("");
        try {
            if (!metadata.isBlank()) return Instant.parse(metadata);
        } catch (RuntimeException ignored) {
            // Goldhub does not consistently expose ISO metadata, so the visible publication date is the fallback.
        }
        var matcher = java.util.regex.Pattern.compile("\\b(\\d{1,2} [A-Z][a-z]+ 20\\d{2})\\b").matcher(raw);
        if (matcher.find()) return LocalDate.parse(matcher.group(1), PUBLISHED_FORMAT).atStartOfDay().toInstant(ZoneOffset.UTC);
        return null;
    }

    private String text(Element element, String field) {
        if (element == null || element.text().isBlank()) throw new IllegalStateException(field + " is missing");
        return element.text().trim();
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
