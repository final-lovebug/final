package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestEventPublisher;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestWriter;
import com.ubidict.backend.reviewrequest.implement.ReviewerDuplicationValidator;
import com.ubidict.backend.reviewrequest.implement.ReviewerWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentWriter;
import com.ubidict.backend.reviewrequest.infra.port.ActiveDictionaryVersionQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryReviewReadinessPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentReviewReadinessPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import com.ubidict.backend.reviewrequest.service.model.RequestDictionaryReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.RequestDocumentReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초안에서 리뷰 요청을 시작한다(D-44).
 *
 * <p><b>리뷰 요청과 최초 개정안과 지정 리뷰어를 한 트랜잭션에서 만든다.</b> 나누면 개정안 없는 리뷰 요청이 남는데 그 상태는 실제 흐름에 없다 —
 * REVIEW_REQUEST_PLAN 7절이 임시 API 3개를 없앤 근거가 그것이다.
 *
 * <p>초안 상태를 여기서 바꾸지 않는다. ReviewRequestCreatedEvent를 각 초안 도메인의 리스너가 받아 리뷰요청됨으로 전이한다. 대신 그 전이가 AFTER_COMMIT
 * 비동기라 거기서 던진 예외는 호출자에게 닿지 않으므로, <b>요청을 받는 이 자리에서 자격을 먼저 확인한다</b> — 판정 자체는 초안 도메인에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftReviewRequestService {

    private final ReviewRequestWriter reviewRequestWriter;
    private final RevisionDocumentWriter revisionDocumentWriter;
    private final RevisionDocumentReader revisionDocumentReader;
    private final RevisionDictionaryWriter revisionDictionaryWriter;
    private final RevisionDictionaryReader revisionDictionaryReader;
    private final ReviewerWriter reviewerWriter;
    private final ReviewerDuplicationValidator reviewerDuplicationValidator;
    private final ReviewRequestEventPublisher eventPublisher;
    private final WorkspaceAccessValidator workspaceAccessValidator;
    private final DraftDocumentQueryPort draftDocumentQueryPort;
    private final DraftDocumentReviewReadinessPort draftDocumentReviewReadinessPort;
    private final DraftDictionaryQueryPort draftDictionaryQueryPort;
    private final DraftDictionaryReviewReadinessPort draftDictionaryReviewReadinessPort;
    private final ActiveDictionaryVersionQueryPort activeDictionaryVersionQueryPort;

    @Transactional
    public ReviewRequestResult requestDocumentReview(RequestDocumentReviewCommand command) {
        DraftDocumentSnapshot draft = draftDocumentQueryPort
                .read(command.draftDocumentId())
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_DRAFT_NOT_FOUND));
        workspaceAccessValidator.validateParticipant(draft.workspaceId(), command.requesterId());
        draftDocumentReviewReadinessPort.validateReviewReady(draft.draftDocumentId());
        if (revisionDocumentReader.existsByDraft(draft.draftDocumentId())) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS);
        }

        ReviewRequest reviewRequest = reviewRequestWriter.write(ReviewRequest.create(
                draft.workspaceId(),
                ReviewRequestType.DOCUMENT,
                command.title(),
                command.description(),
                command.requesterId(),
                command.requesterId()));
        revisionDocumentWriter.write(RevisionDocument.create(
                reviewRequest.getId(),
                draft.documentId(),
                draft.baseVersionNo(),
                draft.draftDocumentId(),
                draft.draftBody(),
                command.requesterId()));
        assignReviewers(reviewRequest, command.reviewerMemberIds(), command.requesterId());
        eventPublisher.publishCreated(reviewRequest, draft.draftDocumentId());

        log.info(
                "[DraftReviewRequestService.requestDocumentReview] Document review requested. reviewRequestId={}, draftDocumentId={}, requesterId={}",
                reviewRequest.getId(),
                draft.draftDocumentId(),
                command.requesterId());

        return ReviewRequestResult.from(reviewRequest);
    }

    /**
     * 사전 리뷰 요청은 ADMIN 이상만 할 수 있다(G-2). 개정안의 기준 버전은 <b>지금 활성인 사전집 버전</b>이며 첫 회차는 0이다 — 발행 시점에 그 값과 현재 활성
     * 버전이 어긋나면 DICTIONARY_VERSION_CONFLICT로 막힌다(D-42).
     */
    @Transactional
    public ReviewRequestResult requestDictionaryReview(RequestDictionaryReviewCommand command) {
        DraftDictionarySnapshot draft = draftDictionaryQueryPort
                .read(command.draftDictionaryId())
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_DRAFT_NOT_FOUND));
        workspaceAccessValidator.validateAtLeast(draft.workspaceId(), command.requesterId(), Permission.ADMIN);
        draftDictionaryReviewReadinessPort.validateReviewReady(draft.draftDictionaryId());
        if (revisionDictionaryReader.existsByDraft(draft.draftDictionaryId())) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS);
        }

        ReviewRequest reviewRequest = reviewRequestWriter.write(ReviewRequest.create(
                draft.workspaceId(),
                ReviewRequestType.DICTIONARY,
                command.title(),
                command.description(),
                command.requesterId(),
                command.requesterId()));
        revisionDictionaryWriter.write(RevisionDictionary.create(
                reviewRequest.getId(),
                draft.dictionaryId(),
                activeDictionaryVersionQueryPort.baseVersionNoForNextVersion(draft.workspaceId()),
                draft.draftDictionaryId(),
                command.requesterId()));
        assignReviewers(reviewRequest, command.reviewerMemberIds(), command.requesterId());
        eventPublisher.publishCreated(reviewRequest, draft.draftDictionaryId());

        log.info(
                "[DraftReviewRequestService.requestDictionaryReview] Dictionary review requested. reviewRequestId={}, draftDictionaryId={}, requesterId={}",
                reviewRequest.getId(),
                draft.draftDictionaryId(),
                command.requesterId());

        return ReviewRequestResult.from(reviewRequest);
    }

    /** 지정 리뷰어는 알림·필터 용도이고 정족수 산입과 무관하다(G-4). 요청 안의 중복은 저장 전에 걸러 같은 회원에게 두 행이 생기지 않게 한다. */
    private void assignReviewers(ReviewRequest reviewRequest, List<Long> reviewerMemberIds, Long actorId) {
        for (Long memberId : new LinkedHashSet<>(reviewerMemberIds)) {
            workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);
            reviewerDuplicationValidator.validate(reviewRequest.getId(), memberId);
            reviewerWriter.write(Reviewer.create(reviewRequest.getId(), memberId, actorId));
        }
    }
}
