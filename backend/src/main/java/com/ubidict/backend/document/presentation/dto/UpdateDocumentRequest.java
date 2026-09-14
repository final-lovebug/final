package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.UpdateDocumentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 제목과 라벨만 바꾼다. 본문은 이 경로로 바뀌지 않는다 — 편집은 대조를 실행해 초안을 만드는 별개의 유스케이스다.
 *
 * <p>라벨은 통째로 교체된다. 생략하면 라벨이 모두 떨어진다.
 */
public record UpdateDocumentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5) List<@NotBlank @Size(max = 20) String> labels) {

    public UpdateDocumentCommand toCommand(Long workspaceId, Long documentId, Long memberId) {
        return new UpdateDocumentCommand(workspaceId, documentId, title, labels, memberId);
    }
}
