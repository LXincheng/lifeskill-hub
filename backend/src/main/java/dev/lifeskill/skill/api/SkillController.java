package dev.lifeskill.skill.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lifeskill.skill.api.dto.SkillResponse;
import dev.lifeskill.skill.api.dto.UpdateSkillRequest;
import dev.lifeskill.skill.application.SkillApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillApplicationService service;

    public SkillController(SkillApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{skillId}")
    public SkillResponse get(@PathVariable UUID skillId) {
        return SkillResponse.from(service.get(skillId));
    }

    @PatchMapping("/{skillId}")
    public SkillResponse update(
            @PathVariable UUID skillId,
            @Valid @RequestBody UpdateSkillRequest request) {
        return SkillResponse.from(service.update(skillId, request.toCommand()));
    }
}
