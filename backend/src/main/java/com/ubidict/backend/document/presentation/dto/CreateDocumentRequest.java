package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 파일 업로드가 아니라 JSON 본문 작성이다. 본문은 v1 버전으로 발행된다.
 */
public record CreateDocumentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        @Size(max = 5) List<@NotBlank @Size(max = 20) String> labels) {

    public CreateDocumentCommand toCommand(Long workspaceId, Long memberId) {
        return new CreateDocumentCommand(workspaceId, title, content, labels, memberId);
    }
}
