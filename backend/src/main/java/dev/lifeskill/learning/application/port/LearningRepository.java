package dev.lifeskill.learning.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.lifeskill.learning.domain.ContentItem;
import dev.lifeskill.learning.domain.LearningFolder;

public interface LearningRepository {
    List<LearningFolder> findFolders();
    Optional<LearningFolder> findFolder(UUID folderId);
    LearningFolder saveFolder(LearningFolder folder);
    void deleteFolder(UUID folderId);
    List<ContentItem> findContentByFolder(UUID folderId);
    List<ContentItem> findContentBySourceRun(UUID sourceRunId);
    Optional<ContentItem> findContent(UUID contentId);
    ContentItem saveContent(ContentItem content);
    void deleteContent(UUID contentId);
}
