package com.ubidict.backend.document.service.model;

import java.util.List;

/**
 * 제목과 라벨만 바꾼다. 본문은 담지 않는다 — 본문 편집은 대조를 실행해 초안을 만드는 별개의 유스케이스다.
 */
public record UpdateDocumentCommand(
        Long workspaceId, Long documentId, String title, List<String> labels, Long memberId) {

    public UpdateDocumentCommand {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
