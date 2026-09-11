package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.SubmitRevisionCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitRevisionDictionaryRequest(
        Long dictionaryId,
        @Min(0) int baseVersionNo,
        @NotNull Long draftDictionaryId) {
    public SubmitRevisionCommand toCommand(Long reviewRequestId, Long actorId) {
        return new SubmitRevisionCommand(
                reviewRequestId, dictionaryId, baseVersionNo, draftDictionaryId, null, actorId);
    }
}
