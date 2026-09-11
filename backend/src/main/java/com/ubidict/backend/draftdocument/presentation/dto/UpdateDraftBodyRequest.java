package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateDraftBodyRequest(@NotBlank String draftBody) {

    public UpdateDraftBodyCommand toCommand(Long draftDocumentId, Long memberId) {
        return new UpdateDraftBodyCommand(draftDocumentId, draftBody, memberId);
    }
}
