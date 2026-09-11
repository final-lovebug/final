package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionTypeValidator;
import com.ubidict.backend.reviewrequest.service.model.RevisionResult;
import com.ubidict.backend.reviewrequest.service.model.SubmitRevisionCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevisionService {
    private final RevisionDocumentReader documents;
    private final RevisionDocumentWriter documentWriter;
    private final ReviewRequestReader requestReader;
    private final RevisionTypeValidator typeValidator;

    @Transactional
    public RevisionResult submitDocument(SubmitRevisionCommand command) {
        typeValidator.validate(requestReader.read(command.reviewRequestId()), true);
        if (documents.exists(command.reviewRequestId(), 0)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS);
        }
        return RevisionResult.from(documentWriter.write(RevisionDocument.create(
                command.reviewRequestId(),
                command.targetId(),
                command.baseVersionNo(),
                command.draftId(),
                command.proposedBody(),
                command.actorId())));
    }

    @Transactional(readOnly = true)
    public List<RevisionResult> documents(Long requestId, Integer round) {
        return documents.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }
}
