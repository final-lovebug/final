package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 문서 초안을 새로 만들 수 있는 상태인지 본다.
 *
 * <p><b>사전집 초안이 진행 중이어도 막지 않는다</b>(D-93). `G-14`의 상호 배타는 개정본이 대조하지 않은 사전집 버전을 기준으로 찍히는 것을 막으려던
 * 것인데, 그 구멍은 대조 기준 버전을 초안에 고정하는 것으로 닫았다({@code DraftDocument.dictionaryVersionNo}). 반대 방향(문서 초안이
 * 진행 중이면 사전집 초안을 만들 수 없다)은 그대로 둔다.
 */
@Component
@RequiredArgsConstructor
public class DraftDocumentCreationPolicyValidator {

    private final DraftDocumentRepository draftDocumentRepository;
    private final ReviewRequestQueryPort reviewRequestQueryPort;

    public void validate(Long documentId) {
        if (draftDocumentRepository.existsByDocumentIdAndStatusNotAndDeletedAtIsNull(
                documentId, DraftDocumentStatus.REVISED)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXISTS);
        }
        if (reviewRequestQueryPort.hasOngoingDocumentReview(documentId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_UNDER_REVIEW);
        }
    }
}
