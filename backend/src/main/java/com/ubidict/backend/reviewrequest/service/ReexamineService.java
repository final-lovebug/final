package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Reexamine;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.ApprovalAuthorityValidator;
import com.ubidict.backend.reviewrequest.implement.ReexamineReader;
import com.ubidict.backend.reviewrequest.implement.ReexamineRoundCalculator;
import com.ubidict.backend.reviewrequest.implement.ReexamineWriter;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentWriter;
import com.ubidict.backend.reviewrequest.service.model.PerformReexamineCommand;
import com.ubidict.backend.reviewrequest.service.model.ReexamineResult;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReexamineService {

    private final ReviewRequestReader reviewRequestReader;
    private final RevisionDocumentReader revisionDocumentReader;
    private final RevisionDocumentWriter revisionDocumentWriter;
    private final RevisionDictionaryReader revisionDictionaryReader;
    private final RevisionDictionaryWriter revisionDictionaryWriter;
    private final ReexamineReader reexamineReader;
    private final ReexamineWriter reexamineWriter;
    private final ReexamineRoundCalculator reexamineRoundCalculator;
    private final ReviewRequestWriter reviewRequestWriter;
    private final ApprovalAuthorityValidator approvalAuthorityValidator;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional
    public ReexamineResult perform(PerformReexamineCommand command) {
        ReviewRequest reviewRequest = reviewRequestReader.read(command.reviewRequestId());
        approvalAuthorityValidator.validateReexamine(reviewRequest, command.actorId());
        reviewRequest = lockForRevision(command.reviewRequestId());
        validateReexaminable(reviewRequest);

        int round = reexamineRoundCalculator.nextRound(currentRound(reviewRequest));
        appendRevision(reviewRequest, round, command);
        Reexamine reexamine = reexamineWriter.write(
                Reexamine.perform(reviewRequest.getId(), round, command.actorId(), command.addressedCommentIds()));
        reviewRequest.resumeReview();

        log.info(
                "[ReexamineService.perform] Review request reexamined. reviewRequestId={}, round={}, actorId={}",
                reviewRequest.getId(),
                round,
                command.actorId());

        return ReexamineResult.from(reexamine);
    }

    @Transactional(readOnly = true)
    public List<ReexamineResult> list(Long reviewRequestId, Long memberId) {
        ReviewRequest reviewRequest = reviewRequestReader.read(reviewRequestId);
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);
        return reexamineReader.readAll(reviewRequestId).stream()
                .map(ReexamineResult::from)
                .toList();
    }

    private int currentRound(ReviewRequest reviewRequest) {
        return reviewRequest.getType() == ReviewRequestType.DOCUMENT
                ? revisionDocumentReader.currentRound(reviewRequest.getId())
                : revisionDictionaryReader.currentRound(reviewRequest.getId());
    }

    private void appendRevision(ReviewRequest reviewRequest, int round, PerformReexamineCommand command) {
        if (reviewRequest.getType() == ReviewRequestType.DOCUMENT) {
            RevisionDocument previous = revisionDocumentReader.readCurrent(reviewRequest.getId());
            revisionDocumentWriter.write(
                    RevisionDocument.reexamine(previous, round, command.proposedBody(), command.actorId()));
            return;
        }

        RevisionDictionary previous = revisionDictionaryReader.readCurrent(reviewRequest.getId());
        revisionDictionaryWriter.write(RevisionDictionary.reexamine(previous, round, command.actorId()));
    }

    private static void validateReexaminable(ReviewRequest reviewRequest) {
        if (!reviewRequest.isReexaminable()) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REEXAMINABLE);
        }
    }

    private ReviewRequest lockForRevision(Long reviewRequestId) {
        try {
            ReviewRequest reviewRequest = reviewRequestReader.readForRevision(reviewRequestId);
            reviewRequestWriter.flush();
            return reviewRequest;
        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_CONCURRENT_MODIFICATION);
        }
    }
}
