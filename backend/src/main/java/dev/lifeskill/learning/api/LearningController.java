package dev.lifeskill.learning.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lifeskill.learning.api.dto.ContentItemRequest;
import dev.lifeskill.learning.api.dto.ContentItemResponse;
import dev.lifeskill.learning.api.dto.LearningFolderRequest;
import dev.lifeskill.learning.api.dto.LearningFolderResponse;
import dev.lifeskill.learning.api.dto.LearningAttemptRequest;
import dev.lifeskill.learning.api.dto.LearningAttemptResponse;
import dev.lifeskill.learning.api.dto.LearningProgressResponse;
import dev.lifeskill.learning.api.dto.LearningAnnotationRequest;
import dev.lifeskill.learning.api.dto.LearningAnnotationResponse;
import dev.lifeskill.learning.application.LearningApplicationService;
import dev.lifeskill.learning.application.LearningContentRevisionService;
import dev.lifeskill.learning.domain.ContentItemType;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LearningController {
    private final LearningApplicationService service;
    private final LearningContentRevisionService revisions;

    public LearningController(LearningApplicationService service, LearningContentRevisionService revisions) {
        this.service = service;
        this.revisions = revisions;
    }

    @GetMapping("/learning-folders")
    public List<LearningFolderResponse> listFolders() {
        return service.listFolders().stream().map(LearningFolderResponse::from).toList();
    }

    @PostMapping("/learning-folders")
    public ResponseEntity<LearningFolderResponse> createFolder(@Valid @RequestBody LearningFolderRequest request) {
        var response = LearningFolderResponse.from(service.createFolder(request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/learning-folders/" + response.id())).body(response);
    }

    @PatchMapping("/learning-folders/{folderId}")
    public LearningFolderResponse updateFolder(
            @PathVariable UUID folderId,
            @Valid @RequestBody LearningFolderRequest request) {
        return LearningFolderResponse.from(service.updateFolder(folderId, request.name(), request.description()));
    }

    @DeleteMapping("/learning-folders/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID folderId) {
        service.deleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/learning-folders/{folderId}/content-items")
    public List<ContentItemResponse> listContent(@PathVariable UUID folderId) {
        return service.listContent(folderId).stream().map(ContentItemResponse::from).toList();
    }

    @GetMapping("/learning-folders/{folderId}/progress")
    public LearningProgressResponse getProgress(@PathVariable UUID folderId) {
        return LearningProgressResponse.from(service.getProgress(folderId));
    }

    @PostMapping("/learning-folders/{folderId}/content-items")
    public ResponseEntity<ContentItemResponse> createContent(
            @PathVariable UUID folderId,
            @Valid @RequestBody ContentItemRequest request) {
        ContentItemType type = request.type() == null ? ContentItemType.ARTICLE : request.type();
        var response = ContentItemResponse.from(service.createContent(folderId, type, request.title(), request.body()));
        return ResponseEntity.created(URI.create("/api/content-items/" + response.id())).body(response);
    }

    @GetMapping("/content-items/{contentId}")
    public ContentItemResponse getContent(@PathVariable UUID contentId) {
        return ContentItemResponse.from(service.getContent(contentId));
    }

    @PatchMapping("/content-items/{contentId}")
    public ContentItemResponse updateContent(
            @PathVariable UUID contentId,
            @Valid @RequestBody ContentItemRequest request) {
        return ContentItemResponse.from(service.updateContent(contentId, request.type(), request.title(), request.body()));
    }

    @GetMapping("/content-items/{contentId}/attempts")
    public List<LearningAttemptResponse> listAttempts(@PathVariable UUID contentId) {
        return service.listAttempts(contentId).stream().map(LearningAttemptResponse::from).toList();
    }

    @PostMapping("/content-items/{contentId}/attempts")
    public ResponseEntity<LearningAttemptResponse> recordAttempt(
            @PathVariable UUID contentId,
            @Valid @RequestBody LearningAttemptRequest request) {
        var response = LearningAttemptResponse.from(service.recordAttempt(
                contentId, request.kind(), request.status(), request.completedUnits(), request.totalUnits(),
                request.completedUnitIndexes()));
        return ResponseEntity.created(URI.create("/api/content-items/" + contentId + "/attempts/" + response.id()))
                .body(response);
    }

    @GetMapping("/content-items/{contentId}/annotations")
    public List<LearningAnnotationResponse> listAnnotations(@PathVariable UUID contentId) {
        return service.listAnnotations(contentId).stream().map(LearningAnnotationResponse::from).toList();
    }

    @PostMapping("/content-items/{contentId}/annotations")
    public ResponseEntity<LearningAnnotationResponse> addAnnotation(
            @PathVariable UUID contentId,
            @Valid @RequestBody LearningAnnotationRequest request) {
        var response = LearningAnnotationResponse.from(service.addAnnotation(
                contentId, request.kind(), request.selectedText(), request.note()));
        return ResponseEntity.created(URI.create(
                "/api/content-items/" + contentId + "/annotations/" + response.id())).body(response);
    }

    @DeleteMapping("/content-items/{contentId}/annotations/{annotationId}")
    public ResponseEntity<Void> deleteAnnotation(
            @PathVariable UUID contentId, @PathVariable UUID annotationId) {
        service.deleteAnnotation(contentId, annotationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/content-items/{contentId}/regenerations")
    public ContentItemResponse regenerate(@PathVariable UUID contentId) {
        return ContentItemResponse.from(revisions.regenerate(contentId));
    }

    @DeleteMapping("/content-items/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId) {
        service.deleteContent(contentId);
        return ResponseEntity.noContent().build();
    }
}
