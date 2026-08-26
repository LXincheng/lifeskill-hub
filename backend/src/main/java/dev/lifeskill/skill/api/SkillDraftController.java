package dev.lifeskill.skill.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lifeskill.skill.api.dto.SkillConfirmationResponse;
import dev.lifeskill.skill.application.SkillConfirmationApplicationService;

@RestController
@RequestMapping("/api/skill-drafts")
public class SkillDraftController {

    private final SkillConfirmationApplicationService confirmationService;

    public SkillDraftController(SkillConfirmationApplicationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/{draftId}/confirmations")
    public ResponseEntity<SkillConfirmationResponse> confirm(
            @PathVariable UUID draftId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        var result = confirmationService.confirm(draftId, idempotencyKey);
        var response = SkillConfirmationResponse.from(result);
        if (result.created()) {
            return ResponseEntity
                    .created(URI.create("/api/skills/" + response.skillId()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }
}
