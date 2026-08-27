package dev.lifeskill.learning.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.lifeskill.learning.application.port.LearningRepository;
import dev.lifeskill.learning.domain.ContentItem;
import dev.lifeskill.learning.domain.LearningFolder;

@Repository
class JpaLearningRepository implements LearningRepository {
    private final SpringDataLearningFolderRepository folderRepository;
    private final SpringDataContentItemRepository contentRepository;

    JpaLearningRepository(
            SpringDataLearningFolderRepository folderRepository,
            SpringDataContentItemRepository contentRepository) {
        this.folderRepository = folderRepository;
        this.contentRepository = contentRepository;
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
}
