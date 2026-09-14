package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import jakarta.validation.constraints.NotNull;

public record CreateCheckJobRequest(@NotNull Long documentId) {

    public CreateCheckJobCommand toCommand(Long memberId) {
        return new CreateCheckJobCommand(documentId, memberId);
    }
}
