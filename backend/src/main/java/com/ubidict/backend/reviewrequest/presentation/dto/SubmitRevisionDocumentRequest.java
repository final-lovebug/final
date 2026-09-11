package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.SubmitRevisionCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitRevisionDocumentRequest(
        @NotNull Long documentId,
        @Min(0) int baseVersionNo,
        @NotNull Long draftDocumentId,
        @NotBlank String proposedBody) {
    public SubmitRevisionCommand toCommand(Long reviewRequestId, Long actorId) {
        return new SubmitRevisionCommand(
                reviewRequestId, documentId, baseVersionNo, draftDocumentId, proposedBody, actorId);
    }
}
