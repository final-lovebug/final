package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.port.ActiveDictionaryVersionQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DictionaryVersionPublishPort;
import com.ubidict.backend.reviewrequest.infra.port.DocumentVersionPublishPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviseProcessor {

    private final DraftDocumentQueryPort draftDocumentQueryPort;
    private final DraftDictionaryQueryPort draftDictionaryQueryPort;
    private final ActiveDictionaryVersionQueryPort activeDictionaryVersionQueryPort;
    private final DocumentVersionPublishPort documentVersionPublishPort;
    private final DictionaryVersionPublishPort dictionaryVersionPublishPort;

    public int processDocument(ReviewRequest request, RevisionDocument revision, Long actorId) {
        DraftDocumentSnapshot draft = draftDocumentQueryPort
                .read(revision.getDraftDocumentId())
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_NOT_FOUND));
        if (!draft.documentId().equals(revision.getDocumentId())
                || draft.baseVersionNo() != revision.getBaseVersionNo()) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_NOT_FOUND);
        }

        int dictionaryVersionNo = activeDictionaryVersionQueryPort.activeVersionNo(request.getWorkspaceId());
        int resultVersionNo = documentVersionPublishPort.publish(
                revision.getDocumentId(),
                revision.getBaseVersionNo(),
                revision.getProposedBody(),
                dictionaryVersionNo,
                actorId);
        revision.recordResult(resultVersionNo);
        return resultVersionNo;
    }

    public int processDictionary(ReviewRequest request, RevisionDictionary revision, Long actorId) {
        DraftDictionarySnapshot draft = draftDictionaryQueryPort
                .read(revision.getDraftDictionaryId())
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_NOT_FOUND));
        if (!draft.workspaceId().equals(request.getWorkspaceId())
                || !Objects.equals(draft.dictionaryId(), revision.getDictionaryId())) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_NOT_FOUND);
        }

        int resultVersionNo = dictionaryVersionPublishPort.publish(
                request.getWorkspaceId(),
                revision.getBaseVersionNo(),
                draftDictionaryQueryPort.readFinalTerms(revision.getDraftDictionaryId()),
                actorId);
        revision.recordResult(resultVersionNo);
        return resultVersionNo;
    }
}
