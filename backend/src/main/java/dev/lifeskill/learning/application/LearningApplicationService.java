package dev.lifeskill.learning.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lifeskill.learning.application.port.LearningRepository;
import dev.lifeskill.learning.domain.ContentItem;
import dev.lifeskill.learning.domain.ContentItemType;
import dev.lifeskill.learning.domain.LearningFolder;
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
