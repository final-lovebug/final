package com.ubidict.backend.reviewrequest.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CommentRepository commentRepository;

    @DisplayName("리뷰 요청에 달린 코멘트를 리뷰 조인으로 조회한다.")
    @Test
    void findByReviewRequestId() {
        // given
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰 요청", null, 1L, 1L));
        Review review = reviewRepository.save(Review.submit(request.getId(), 2L, 0, ReviewVerdict.APPROVED, 2L));
        Comment expected = commentRepository.save(Comment.create(review.getId(), 2L, "의견", null, 10L, null, 2L));
        commentRepository.save(Comment.create(review.getId(), 2L, "다른 항목", null, 20L, null, 2L));
        em.flush();
        em.clear();

        // when
        var comments = commentRepository.findByReviewRequestId(request.getId(), false, 10L);

        // then
        assertThat(comments).extracting(Comment::getId).containsExactly(expected.getId());
    }
}
