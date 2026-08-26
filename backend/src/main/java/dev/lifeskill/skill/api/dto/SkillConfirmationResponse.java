package dev.lifeskill.skill.api.dto;

import java.time.Instant;
import java.util.UUID;

import dev.lifeskill.skill.application.SkillConfirmationResult;

public record SkillConfirmationResponse(
        UUID draftId,
        String draftStatus,
        UUID skillId,
        String skillName,
        String skillStatus,
        int currentVersion,
        Instant confirmedAt) {

    public static SkillConfirmationResponse from(SkillConfirmationResult result) {
        return new SkillConfirmationResponse(
                result.draft().id(),
                result.draft().status().name(),
                result.skill().id(),
                result.skill().name(),
                result.skill().status().name(),
                result.version().version(),
                result.draft().confirmedAt());
    }
}
