package dev.lifeskill.learning.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.lifeskill.learning.application.port.LearningRepository;
import dev.lifeskill.learning.domain.ContentItem;
import dev.lifeskill.learning.domain.LearningFolder;
import dev.lifeskill.learning.domain.LearningAttempt;
import dev.lifeskill.learning.domain.LearningAnnotation;

@Repository
class JpaLearningRepository implements LearningRepository {
    private final SpringDataLearningFolderRepository folderRepository;
    private final SpringDataContentItemRepository contentRepository;
    private final SpringDataLearningAttemptRepository attemptRepository;
    private final SpringDataLearningAnnotationRepository annotationRepository;

    JpaLearningRepository(
            SpringDataLearningFolderRepository folderRepository,
            SpringDataContentItemRepository contentRepository,
            SpringDataLearningAttemptRepository attemptRepository,
            SpringDataLearningAnnotationRepository annotationRepository) {
        this.folderRepository = folderRepository;
        this.contentRepository = contentRepository;
        this.attemptRepository = attemptRepository;
        this.annotationRepository = annotationRepository;
    }

    @Override
    public List<LearningFolder> findFolders() {
        return folderRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<LearningFolder> findFolder(UUID folderId) {
        return folderRepository.findById(folderId).map(this::toDomain);
    }

    @Override
    public LearningFolder saveFolder(LearningFolder folder) {
        return toDomain(folderRepository.save(new LearningFolderEntity(
                folder.id(), folder.name(), folder.description(), folder.createdAt(), folder.updatedAt())));
    }

    @Override
    public void deleteFolder(UUID folderId) {
        contentRepository.deleteAllByFolderId(folderId);
        folderRepository.deleteById(folderId);
    }

    @Override
    public List<ContentItem> findContentByFolder(UUID folderId) {
        return contentRepository.findAllByFolderIdOrderByUpdatedAtDesc(folderId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ContentItem> findContent(UUID contentId) {
        return contentRepository.findById(contentId).map(this::toDomain);
    }

    @Override
    public List<ContentItem> findContentBySourceRun(UUID sourceRunId) {
        return contentRepository.findAllBySourceSkillRunIdOrderByUpdatedAtAsc(sourceRunId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public ContentItem saveContent(ContentItem content) {
        return toDomain(contentRepository.save(new ContentItemEntity(
                content.id(), content.folderId(), content.sourceSkillRunId(), content.type(), content.title(),
                Map.of("body", content.body()), content.verificationStatus(), content.createdAt(), content.updatedAt())));
    }

    @Override
    public void deleteContent(UUID contentId) {
        contentRepository.deleteById(contentId);
    }

    @Override
    public LearningAttempt saveAttempt(LearningAttempt attempt) {
        Map<String, Object> result = Map.of("completedUnitIndexes", attempt.completedUnitIndexes());
        java.math.BigDecimal score = attempt.score() == null ? null : java.math.BigDecimal.valueOf(attempt.score());
        return toDomain(attemptRepository.save(new LearningAttemptEntity(
                attempt.id(), attempt.contentItemId(), attempt.kind(), attempt.status(), attempt.completedUnits(),
                attempt.totalUnits(), score, result, attempt.completedAt(), attempt.createdAt())));
    }

    @Override
    public List<LearningAttempt> findAttempts(UUID contentId) {
        return attemptRepository.findAllByContentItemIdOrderByCreatedAtDesc(contentId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<LearningAttempt> findAttemptsForContent(List<UUID> contentIds) {
        if (contentIds.isEmpty()) return List.of();
        return attemptRepository.findAllByContentItemIdInOrderByCreatedAtDesc(contentIds).stream().map(this::toDomain).toList();
    }

    @Override
    public LearningAnnotation saveAnnotation(LearningAnnotation annotation) {
        return toDomain(annotationRepository.save(new LearningAnnotationEntity(
                annotation.id(), annotation.contentItemId(), annotation.kind(), annotation.selectedText(),
                annotation.note(), annotation.createdAt())));
    }

    @Override
    public List<LearningAnnotation> findAnnotations(UUID contentId) {
        return annotationRepository.findAllByContentItemIdOrderByCreatedAtDesc(contentId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void deleteAnnotation(UUID annotationId) {
        annotationRepository.deleteById(annotationId);
    }

    private LearningFolder toDomain(LearningFolderEntity entity) {
        return new LearningFolder(entity.id(), entity.name(), entity.description(), entity.createdAt(), entity.updatedAt());
    }

    private ContentItem toDomain(ContentItemEntity entity) {
        Object body = entity.payload().get("body");
        if (!(body instanceof String text)) throw new IllegalStateException("Content item body is missing");
        return new ContentItem(
                entity.id(), entity.folderId(), entity.sourceSkillRunId(), entity.type(), entity.title(), text,
                entity.verificationStatus(), entity.createdAt(), entity.updatedAt());
    }

    private LearningAttempt toDomain(LearningAttemptEntity entity) {
        Object rawIndexes = entity.result().get("completedUnitIndexes");
        List<Integer> indexes = rawIndexes instanceof List<?> values
                ? values.stream().filter(Number.class::isInstance).map(Number.class::cast).map(Number::intValue).toList()
                : List.of();
        return new LearningAttempt(
                entity.id(), entity.contentItemId(), entity.kind(), entity.status(), entity.completedUnits(),
                entity.totalUnits(), entity.score() == null ? null : entity.score().doubleValue(), indexes,
                entity.completedAt(), entity.createdAt());
    }

    private LearningAnnotation toDomain(LearningAnnotationEntity entity) {
        return new LearningAnnotation(
                entity.id(), entity.contentItemId(), entity.kind(), entity.selectedText(), entity.note(), entity.createdAt());
    }
}
