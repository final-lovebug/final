package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.CommentReader;
import com.ubidict.backend.reviewrequest.implement.CommentWriter;
import com.ubidict.backend.reviewrequest.implement.ReviewReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewWriter;
import com.ubidict.backend.reviewrequest.service.model.ReviewResult;
import com.ubidict.backend.reviewrequest.service.model.SubmitReviewCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRequestReader reviewRequestReader;
    private final ReviewReader reviewReader;
    private final ReviewWriter reviewWriter;
    private final CommentReader commentReader;
    private final CommentWriter commentWriter;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional
    public ReviewResult submit(SubmitReviewCommand command) {
        ReviewRequest reviewRequest = reviewRequestReader.read(command.reviewRequestId());
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), command.memberId());

        Review review = reviewWriter.write(Review.submit(
                reviewRequest.getId(),
                command.memberId(),
                command.targetRound(),
                command.verdict(),
                command.memberId()));
        writeComments(review, command);

        log.info(
                "[ReviewService.submit] Review submitted. reviewRequestId={}, reviewId={}, memberId={}, verdict={}",
                reviewRequest.getId(),
                review.getId(),
                command.memberId(),
                command.verdict());

        return ReviewResult.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResult> list(Long reviewRequestId, Long memberId, Integer targetRound) {
        ReviewRequest reviewRequest = reviewRequestReader.read(reviewRequestId);
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);

        return reviewReader.readAll(reviewRequestId, targetRound).stream()
                .map(ReviewResult::from)
                .toList();
    }

    private void writeComments(Review review, SubmitReviewCommand command) {
        for (SubmitReviewCommand.NewComment newComment : command.comments()) {
            validateCommentParent(newComment.parentId(), review.getId());
            commentWriter.write(Comment.create(
                    review.getId(),
                    command.memberId(),
                    newComment.content(),
                    newComment.anchor(),
                    newComment.targetItemId(),
                    newComment.parentId(),
                    command.memberId()));
        }
    }

    private void validateCommentParent(Long parentId, Long reviewId) {
        if (parentId == null) {
            return;
        }
        Comment parent = commentReader.read(parentId);
        if (!parent.getReviewId().equals(reviewId)) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_COMMENT_PARENT);
        }
    }
}
