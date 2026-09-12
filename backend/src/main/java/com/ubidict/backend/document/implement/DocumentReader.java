package com.ubidict.backend.document.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.infra.DocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 삭제되지 않은, 그리고 해당 워크스페이스에 속한 문서만 읽는다.
 *
 * <p>다른 워크스페이스의 문서 식별자는 「권한 없음」이 아니라 「없음」으로 다룬다. 남의 워크스페이스에 그 문서가 있다는 사실이 드러나지 않아야 한다.
 */
@Component
@RequiredArgsConstructor
public class DocumentReader {

    private final DocumentRepository documentRepository;

    public Document read(Long documentId) {
        return documentRepository
                .findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
    }

    public Document read(Long documentId, Long workspaceId) {
        return documentRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(documentId, workspaceId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
    }

    public List<Document> readAll(Long workspaceId) {
        return documentRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId);
    }

    public List<Document> readAllByLabel(Long workspaceId, String labelName) {
        return documentRepository.findAllByWorkspaceIdAndLabelName(workspaceId, labelName);
    }
}
