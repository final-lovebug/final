package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestRemover;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestWriter;
import com.ubidict.backend.reviewrequest.service.model.CancelReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.CreateReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestSearchQuery;
import com.ubidict.backend.reviewrequest.service.model.UpdateReviewRequestCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
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

    @Transactional(readOnly = true)
    public PageResult<ReviewRequestResult> search(ReviewRequestSearchQuery query) {
        return reviewRequestReader.search(query).map(ReviewRequestResult::from);
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

        return ReviewRequestResult.from(reviewRequest);
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
        reviewRequestRemover.remove(reviewRequest, command.actorId());

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
}
