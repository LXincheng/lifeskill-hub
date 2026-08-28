package dev.lifeskill.learning.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.learning.application.port.LearningRepository;
import dev.lifeskill.learning.domain.ContentItem;
import dev.lifeskill.learning.domain.ContentItemType;
import dev.lifeskill.learning.domain.LearningFolder;
import dev.lifeskill.learning.domain.LearningAttempt;
import dev.lifeskill.learning.domain.LearningAttemptKind;
import dev.lifeskill.learning.domain.LearningAttemptStatus;
import dev.lifeskill.shared.application.IdGenerator;

@Service
public class LearningApplicationService {
    private final LearningRepository repository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public LearningApplicationService(LearningRepository repository, Clock clock, IdGenerator idGenerator) {
        this.repository = repository;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<LearningFolder> listFolders() {
        return repository.findFolders();
    }

    @Transactional
    public LearningFolder createFolder(String name, String description) {
        var now = clock.instant();
        return repository.saveFolder(new LearningFolder(idGenerator.nextId(), name, description, now, now));
    }

    @Transactional
    public LearningFolder updateFolder(UUID folderId, String name, String description) {
        LearningFolder folder = requireFolder(folderId);
        return repository.saveFolder(folder.update(name, description, clock.instant()));
    }

    @Transactional
    public void deleteFolder(UUID folderId) {
        requireFolder(folderId);
        repository.deleteFolder(folderId);
    }

    @Transactional(readOnly = true)
    public List<ContentItem> listContent(UUID folderId) {
        requireFolder(folderId);
        return repository.findContentByFolder(folderId);
    }

    @Transactional(readOnly = true)
    public ContentItem getContent(UUID contentId) {
        return requireContent(contentId);
    }

    @Transactional
    public ContentItem createContent(UUID folderId, ContentItemType type, String title, String body) {
        requireFolder(folderId);
        var now = clock.instant();
        return repository.saveContent(new ContentItem(idGenerator.nextId(), folderId, type, title, body, now, now));
    }

    @Transactional
    public LearningAttempt recordAttempt(
            UUID contentId,
            LearningAttemptKind kind,
            LearningAttemptStatus status,
            int completedUnits,
            int totalUnits,
            List<Integer> completedUnitIndexes) {
        ContentItem content = requireContent(contentId);
        if (kind == LearningAttemptKind.QUIZ && content.type() != ContentItemType.QUIZ) {
            throw new IllegalArgumentException("Quiz attempts can only target quiz content");
        }
        if (kind == LearningAttemptKind.PROGRESS && content.type() == ContentItemType.QUIZ) {
            throw new IllegalArgumentException("Quiz progress must be recorded as a quiz attempt");
        }
        var now = clock.instant();
        Double score = kind == LearningAttemptKind.QUIZ
                ? Math.round(completedUnits * 10_000.0 / totalUnits) / 100.0
                : null;
        return repository.saveAttempt(new LearningAttempt(
                idGenerator.nextId(), contentId, kind, status, completedUnits, totalUnits, score,
                completedUnitIndexes, status == LearningAttemptStatus.COMPLETED ? now : null, now));
    }

    @Transactional(readOnly = true)
    public List<LearningAttempt> listAttempts(UUID contentId) {
        requireContent(contentId);
        return repository.findAttempts(contentId);
    }

    @Transactional(readOnly = true)
    public LearningProgress getProgress(UUID folderId) {
        List<ContentItem> content = listContent(folderId);
        List<LearningAttempt> attempts = repository.findAttemptsForContent(content.stream().map(ContentItem::id).toList());
        java.util.Map<UUID, LearningAttempt> latest = new java.util.LinkedHashMap<>();
        attempts.forEach(attempt -> latest.putIfAbsent(attempt.contentItemId(), attempt));
        int started = latest.size();
        int completed = (int) latest.values().stream()
                .filter(attempt -> attempt.status() == LearningAttemptStatus.COMPLETED).count();
        var quizScores = latest.values().stream()
                .filter(attempt -> attempt.kind() == LearningAttemptKind.QUIZ && attempt.score() != null)
                .mapToDouble(LearningAttempt::score).toArray();
        Double average = quizScores.length == 0 ? null
                : Math.round(java.util.Arrays.stream(quizScores).average().orElse(0) * 100.0) / 100.0;
        int completionPercent = content.isEmpty() ? 0 : Math.round(completed * 100f / content.size());
        java.time.Instant latestActivity = attempts.isEmpty() ? null : attempts.getFirst().createdAt();
        return new LearningProgress(content.size(), started, completed, completionPercent, average, latestActivity);
    }

    public record LearningProgress(
            int contentCount, int startedCount, int completedCount, int completionPercent,
            Double averageQuizScore, java.time.Instant latestActivityAt) {}

    @Transactional
    public GeneratedLearningBundle createVerifiedBundle(
            UUID sourceRunId,
            String folderName,
            String folderDescription,
            String pathTitle,
            String pathBody,
            String articleTitle,
            String articleBody,
            String quizTitle,
            String quizBody) {
        var now = clock.instant();
        LearningFolder folder = repository.saveFolder(new LearningFolder(
                idGenerator.nextId(), folderName, folderDescription, now, now));
        List<ContentItem> items = List.of(
                verifiedContent(folder.id(), sourceRunId, ContentItemType.LEARNING_PATH, pathTitle, pathBody, now),
                verifiedContent(folder.id(), sourceRunId, ContentItemType.ARTICLE, articleTitle, articleBody, now),
                verifiedContent(folder.id(), sourceRunId, ContentItemType.QUIZ, quizTitle, quizBody, now));
        return new GeneratedLearningBundle(folder, items);
    }

    @Transactional
    public GeneratedLearningBundle createPlannedBundle(
            UUID sourceRunId,
            dev.lifeskill.agent.application.port.AgentModelPort.LearningResult plan) {
        var now = clock.instant();
        LearningFolder folder = repository.saveFolder(new LearningFolder(
                idGenerator.nextId(), plan.folderName(), plan.folderDescription(), now, now));
        List<ContentItem> items = List.of(
                plannedContent(folder.id(), sourceRunId, ContentItemType.LEARNING_PATH, plan.pathTitle(), plan.pathBody(), now),
                plannedContent(folder.id(), sourceRunId, ContentItemType.ARTICLE, plan.articleTitle(), plan.articleBody(), now),
                plannedContent(folder.id(), sourceRunId, ContentItemType.QUIZ, plan.quizTitle(), plan.quizBody(), now));
        return new GeneratedLearningBundle(folder, items);
    }

    @Transactional
    public ContentItem createVerifiedReport(UUID sourceRunId, String folderName, String description,
                                            String reportTitle, String reportBody) {
        Optional<GeneratedLearningBundle> existing = findVerifiedBundle(sourceRunId);
        if (existing.isPresent()) {
            return existing.get().contentItems().stream()
                    .filter(item -> item.type() == ContentItemType.REPORT)
                    .findFirst().orElseThrow();
        }
        var now = clock.instant();
        LearningFolder folder = repository.saveFolder(new LearningFolder(
                idGenerator.nextId(), folderName, description, now, now));
        return verifiedContent(folder.id(), sourceRunId, ContentItemType.REPORT, reportTitle, reportBody, now);
    }

    @Transactional(readOnly = true)
    public Optional<GeneratedLearningBundle> findVerifiedBundle(UUID sourceRunId) {
        List<ContentItem> items = repository.findContentBySourceRun(sourceRunId);
        if (items.isEmpty()) return Optional.empty();
        LearningFolder folder = requireFolder(items.getFirst().folderId());
        return Optional.of(new GeneratedLearningBundle(folder, items));
    }

    private ContentItem verifiedContent(
            UUID folderId, UUID sourceRunId, ContentItemType type, String title, String body, java.time.Instant now) {
        return repository.saveContent(new ContentItem(
                idGenerator.nextId(), folderId, sourceRunId, type, title, body, "VERIFIED", now, now));
    }

    private ContentItem plannedContent(
            UUID folderId, UUID sourceRunId, ContentItemType type, String title, String body, java.time.Instant now) {
        return repository.saveContent(new ContentItem(
                idGenerator.nextId(), folderId, sourceRunId, type, title, body, "AI_GENERATED", now, now));
    }

    public record GeneratedLearningBundle(LearningFolder folder, List<ContentItem> contentItems) {}

    @Transactional
    public ContentItem updateContent(UUID contentId, ContentItemType type, String title, String body) {
        ContentItem content = requireContent(contentId);
        return repository.saveContent(content.update(type, title, body, clock.instant()));
    }

    @Transactional
    public void deleteContent(UUID contentId) {
        requireContent(contentId);
        repository.deleteContent(contentId);
    }

    private LearningFolder requireFolder(UUID folderId) {
        return repository.findFolder(folderId)
                .orElseThrow(() -> new LearningResourceNotFoundException("Learning folder", folderId));
    }

    private ContentItem requireContent(UUID contentId) {
        return repository.findContent(contentId)
                .orElseThrow(() -> new LearningResourceNotFoundException("Content item", contentId));
    }
}
