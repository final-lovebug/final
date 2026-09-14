package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.implement.ApprovalAuthorityValidator;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestEventPublisher;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestRemover;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestWriter;
import com.ubidict.backend.reviewrequest.implement.ReviewerReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.service.model.CancelReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.CreateReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestSearchQuery;
import com.ubidict.backend.reviewrequest.service.model.UpdateReviewRequestCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewRequestService {

    private final ReviewRequestReader reviewRequestReader;
    private final ReviewRequestWriter reviewRequestWriter;
    private final ReviewRequestRemover reviewRequestRemover;
    private final WorkspaceAccessValidator workspaceAccessValidator;
    private final ApprovalAuthorityValidator approvalAuthorityValidator;
    private final RevisionDocumentReader revisionDocumentReader;
    private final RevisionDictionaryReader revisionDictionaryReader;
    private final ReviewerReader reviewerReader;
    private final ReviewRequestEventPublisher eventPublisher;

    /**
     * 목록 응답에 대상 문서/사전집 id와 리뷰어 수를 실어 보낸다(T-INT-12, D-63) — 프론트가
     * 항목마다 {@code revision-documents}/{@code reviewers}를 따로 호출하지 않도록(N+1 방지).
     * type별로 나눠 필요한 revision 테이블만 배치 조회하고, 리뷰어 수는 한 번에 집계한다.
     */
    @Transactional(readOnly = true)
    public PageResult<ReviewRequestResult> search(ReviewRequestSearchQuery query) {
        PageResult<ReviewRequest> page = reviewRequestReader.search(query);

        List<Long> documentReviewRequestIds = page.content().stream()
                .filter(r -> r.getType() == ReviewRequestType.DOCUMENT)
                .map(ReviewRequest::getId)
                .toList();
        List<Long> dictionaryReviewRequestIds = page.content().stream()
                .filter(r -> r.getType() == ReviewRequestType.DICTIONARY)
                .map(ReviewRequest::getId)
                .toList();
        List<Long> allReviewRequestIds =
                page.content().stream().map(ReviewRequest::getId).toList();

        Map<Long, RevisionDocument> latestDocumentRevisions =
                revisionDocumentReader.readLatestByReviewRequestIds(documentReviewRequestIds);
        Map<Long, RevisionDictionary> latestDictionaryRevisions =
                revisionDictionaryReader.readLatestByReviewRequestIds(dictionaryReviewRequestIds);
        Map<Long, Integer> reviewerCounts = reviewerReader.countsByReviewRequestIds(allReviewRequestIds);

        return page.map(reviewRequest -> ReviewRequestResult.from(
                reviewRequest,
                resolveTargetId(reviewRequest, latestDocumentRevisions, latestDictionaryRevisions),
                reviewerCounts.getOrDefault(reviewRequest.getId(), 0)));
    }

    @Transactional
    public ReviewRequestResult create(CreateReviewRequestCommand command) {
        workspaceAccessValidator.validateParticipant(command.workspaceId(), command.requesterId());

        ReviewRequest reviewRequest = ReviewRequest.create(
                command.workspaceId(),
                command.type(),
                command.title(),
                command.description(),
                command.requesterId(),
                command.requesterId());
        ReviewRequest saved = reviewRequestWriter.write(reviewRequest);

        log.info(
                "[ReviewRequestService.create] Review request created. reviewRequestId={}, workspaceId={}, requesterId={}",
                saved.getId(),
                saved.getWorkspaceId(),
                saved.getRequesterId());

        return ReviewRequestResult.from(saved);
    }

    @Transactional(readOnly = true)
    public ReviewRequestResult read(Long reviewRequestId, Long memberId) {
        ReviewRequest reviewRequest = readAccessible(reviewRequestId, memberId);
        Long targetId = resolveTargetId(
                reviewRequest,
                revisionDocumentReader.readLatestByReviewRequestIds(List.of(reviewRequest.getId())),
                revisionDictionaryReader.readLatestByReviewRequestIds(List.of(reviewRequest.getId())));
        int reviewerCount = reviewerReader
                .countsByReviewRequestIds(List.of(reviewRequest.getId()))
                .getOrDefault(reviewRequest.getId(), 0);

        return ReviewRequestResult.from(reviewRequest, targetId, reviewerCount);
    }

    @Transactional
    public ReviewRequestResult update(UpdateReviewRequestCommand command) {
        ReviewRequest reviewRequest = readAccessible(command.reviewRequestId(), command.actorId());
        reviewRequest.changeContent(command.title(), command.description());

        log.info(
                "[ReviewRequestService.update] Review request updated. reviewRequestId={}, actorId={}",
                reviewRequest.getId(),
                command.actorId());

        return ReviewRequestResult.from(reviewRequest);
    }

    @Transactional
    public ReviewRequestResult cancel(CancelReviewRequestCommand command) {
        ReviewRequest reviewRequest = readAccessible(command.reviewRequestId(), command.actorId());
        approvalAuthorityValidator.validateRequesterOrAdmin(reviewRequest, command.actorId());
        if (reviewRequest.getRequesterId().equals(command.actorId())) {
            reviewRequestRemover.remove(reviewRequest, command.actorId());
        } else {
            reviewRequestRemover.removeByAdministrator(reviewRequest);
        }
        Long sourceDraftId = sourceDraftId(reviewRequest);
        if (sourceDraftId != null) {
            eventPublisher.publishCanceled(reviewRequest, sourceDraftId);
        }

        log.info(
                "[ReviewRequestService.cancel] Review request canceled. reviewRequestId={}, actorId={}",
                reviewRequest.getId(),
                command.actorId());

        return ReviewRequestResult.from(reviewRequest);
    }

    private ReviewRequest readAccessible(Long reviewRequestId, Long memberId) {
        ReviewRequest reviewRequest = reviewRequestReader.read(reviewRequestId);
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);

        return reviewRequest;
    }

    private Long resolveTargetId(
            ReviewRequest reviewRequest,
            Map<Long, RevisionDocument> latestDocumentRevisions,
            Map<Long, RevisionDictionary> latestDictionaryRevisions) {
        if (reviewRequest.getType() == ReviewRequestType.DOCUMENT) {
            RevisionDocument revision = latestDocumentRevisions.get(reviewRequest.getId());
            return revision == null ? null : revision.getDocumentId();
        }
        RevisionDictionary revision = latestDictionaryRevisions.get(reviewRequest.getId());
        return revision == null ? null : revision.getDictionaryId();
    }

    private Long sourceDraftId(ReviewRequest reviewRequest) {
        if (reviewRequest.getType() == ReviewRequestType.DOCUMENT) {
            return revisionDocumentReader.read(reviewRequest.getId()).stream()
                    .max(Comparator.comparingInt(RevisionDocument::getReexamineRound))
                    .map(RevisionDocument::getDraftDocumentId)
                    .orElse(null);
        }
        return revisionDictionaryReader.read(reviewRequest.getId()).stream()
                .max(Comparator.comparingInt(RevisionDictionary::getReexamineRound))
                .map(RevisionDictionary::getDraftDictionaryId)
                .orElse(null);
    }
}
