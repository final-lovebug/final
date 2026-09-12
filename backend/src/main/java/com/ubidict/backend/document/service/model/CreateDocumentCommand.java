package com.ubidict.backend.document.service.model;

import java.util.List;

public record CreateDocumentCommand(
        Long workspaceId, String title, String content, List<String> labels, Long memberId) {

    public CreateDocumentCommand {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
