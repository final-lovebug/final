package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
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

}
