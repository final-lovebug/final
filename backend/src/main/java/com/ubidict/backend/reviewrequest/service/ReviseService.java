package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.Revise;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.ApprovalAuthorityValidator;
import com.ubidict.backend.reviewrequest.implement.LatestReviewAggregator;
import com.ubidict.backend.reviewrequest.implement.LatestReviewAggregator.ReviewAggregate;
import com.ubidict.backend.reviewrequest.implement.ReviewReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestEventPublisher;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestWriter;
import com.ubidict.backend.reviewrequest.implement.ReviseEligibilityCalculator;
import com.ubidict.backend.reviewrequest.implement.ReviseProcessor;
import com.ubidict.backend.reviewrequest.implement.ReviseWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.infra.port.WorkspacePolicyPort;
import com.ubidict.backend.reviewrequest.service.model.PerformReviseCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviseService {

    private final ReviewRequestReader reviewRequestReader;
    private final ReviewReader reviewReader;
    private final RevisionDocumentReader revisionDocumentReader;
    private final RevisionDictionaryReader revisionDictionaryReader;
    private final LatestReviewAggregator latestReviewAggregator;
    private final ReviseEligibilityCalculator reviseEligibilityCalculator;
    private final ReviseProcessor reviseProcessor;
    private final ReviseWriter reviseWriter;
    private final ReviewRequestWriter reviewRequestWriter;
    private final WorkspacePolicyPort workspacePolicyPort;
    private final ApprovalAuthorityValidator approvalAuthorityValidator;
    private final ReviewRequestEventPublisher eventPublisher;

    @Transactional
    public ReviseResult perform(PerformReviseCommand command) {
        ReviewRequest reviewRequest = reviewRequestReader.read(command.reviewRequestId());
        approvalAuthorityValidator.validateRevise(reviewRequest, command.actorId());
        reviewRequest = lockForRevision(command.reviewRequestId());
        validateNotRevised(reviewRequest);
        validateEligible(reviewRequest);

        PublishResult publishResult = publish(reviewRequest, command.actorId());

        Revise revise = reviseWriter.write(
                Revise.perform(reviewRequest.getId(), publishResult.resultVersionNo(), command.actorId()));
        reviewRequest.markRevised(revise.getPerformedAt());
        eventPublisher.publishRevised(reviewRequest, publishResult.targetDraftId(), publishResult.resultVersionNo());

        log.info(
                "[ReviseService.perform] Review request revised. reviewRequestId={}, resultVersionNo={}, actorId={}",
                reviewRequest.getId(),
                publishResult.resultVersionNo(),
                command.actorId());

        return ReviseResult.from(revise);
    }

    private void validateEligible(ReviewRequest reviewRequest) {
        ReviewAggregate aggregate = latestReviewAggregator.aggregate(reviewReader.readLatest(reviewRequest.getId()));
        int requiredReviewerCount =
                workspacePolicyPort.requiredReviewerCount(reviewRequest.getWorkspaceId(), reviewRequest.getType());
        if (!reviseEligibilityCalculator.isEligible(requiredReviewerCount, aggregate)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_ELIGIBLE_FOR_REVISE);
        }
    }

    private static void validateNotRevised(ReviewRequest reviewRequest) {
        if (reviewRequest.isRevised()) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_ALREADY_REVISED);
        }
    }

    private PublishResult publish(ReviewRequest reviewRequest, Long actorId) {
        if (reviewRequest.getType() == ReviewRequestType.DOCUMENT) {
            RevisionDocument revision = revisionDocumentReader.readCurrent(reviewRequest.getId());
            int versionNo = reviseProcessor.processDocument(reviewRequest, revision, actorId);
            return new PublishResult(revision.getDraftDocumentId(), versionNo);
        }

        RevisionDictionary revision = revisionDictionaryReader.readCurrent(reviewRequest.getId());
        int versionNo = reviseProcessor.processDictionary(reviewRequest, revision, actorId);
        return new PublishResult(revision.getDraftDictionaryId(), versionNo);
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

    private record PublishResult(Long targetDraftId, int resultVersionNo) {}
}
