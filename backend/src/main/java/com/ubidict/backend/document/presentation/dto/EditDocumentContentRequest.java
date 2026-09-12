package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.EditDocumentContentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditDocumentContentRequest(
        @NotBlank @Size(max = 10000) String content) {
    public EditDocumentContentCommand toCommand(Long workspaceId, Long documentId, Long memberId) {
        return new EditDocumentContentCommand(workspaceId, documentId, content, memberId);
    }
}
