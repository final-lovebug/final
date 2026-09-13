package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestEventPublisher;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryWriter;
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
    private final RevisionDictionaryReader dictionaries;
    private final RevisionDictionaryWriter dictionaryWriter;
    private final ReviewRequestReader requestReader;
    private final RevisionTypeValidator typeValidator;
    private final ReviewRequestEventPublisher eventPublisher;

    @Transactional
    public RevisionResult submitDocument(SubmitRevisionCommand command) {
        ReviewRequest request = requestReader.read(command.reviewRequestId());
        typeValidator.validate(request, true);
        if (documents.exists(command.reviewRequestId(), 0)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS);
        }
        RevisionDocument revision = documentWriter.write(RevisionDocument.create(
                command.reviewRequestId(),
                command.targetId(),
                command.baseVersionNo(),
                command.draftId(),
                command.proposedBody(),
                command.actorId()));
        eventPublisher.publishCreated(request, revision.getDraftDocumentId());
        return RevisionResult.from(revision);
    }

    @Transactional(readOnly = true)
    public List<RevisionResult> documents(Long requestId, Integer round) {
        return documents.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }

    @Transactional
    public RevisionResult submitDictionary(SubmitRevisionCommand command) {
        ReviewRequest request = requestReader.read(command.reviewRequestId());
        typeValidator.validate(request, false);
        if (dictionaries.exists(command.reviewRequestId(), 0)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS);
        }
        RevisionDictionary revision = dictionaryWriter.write(RevisionDictionary.create(
                command.reviewRequestId(),
                command.targetId(),
                command.baseVersionNo(),
                command.draftId(),
                command.actorId()));
        eventPublisher.publishCreated(request, revision.getDraftDictionaryId());
        return RevisionResult.from(revision);
    }

    @Transactional(readOnly = true)
    public List<RevisionResult> dictionaries(Long requestId, Integer round) {
        return dictionaries.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }
}
