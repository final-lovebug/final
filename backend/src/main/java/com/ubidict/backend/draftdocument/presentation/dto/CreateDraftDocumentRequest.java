package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDraftDocumentRequest(
        @NotNull Long documentId,
        @Positive int baseVersionNo,

        @Schema(description = "Phase 4에 서버 파생으로 전환 예정", deprecated = true) @NotBlank
        String draftBody) {

    public CreateDraftDocumentCommand toCommand(Long memberId) {
        return new CreateDraftDocumentCommand(documentId, baseVersionNo, draftBody, memberId);
    }
}
