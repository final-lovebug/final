package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.CommentRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.service.model.AddCommentCommand;
import com.ubidict.backend.reviewrequest.service.model.ResolveCommentCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentServiceTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 2L;

    @Autowired
    private CommentService commentService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CommentRepository commentRepository;

    @DisplayName("리뷰에 전체 대상 코멘트를 추가한다.")
    @Test
    void addComment() {
        // given
        Review review = saveReview();

        // when
        var result = commentService.add(new AddCommentCommand(review.getId(), MEMBER_ID, "전체 의견", null, null, null));

        // then
        assertThat(result.reviewId()).isEqualTo(review.getId());
        assertThat(result.content()).isEqualTo("전체 의견");
    }

    @DisplayName("같은 리뷰의 코멘트에 답글을 추가하면 트리로 조회된다.")
    @Test
    void addReply() {
        // given
        Review review = saveReview();
        Comment parent =
                commentRepository.save(Comment.create(review.getId(), MEMBER_ID, "부모", null, null, null, MEMBER_ID));

        // when
        commentService.add(new AddCommentCommand(review.getId(), MEMBER_ID, "답글", null, null, parent.getId()));

        // then
        var tree = commentService.list(review.getReviewRequestId(), MEMBER_ID, null, null);
        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().children()).extracting("content").containsExactly("답글");
    }

    @DisplayName("코멘트를 해결 상태로 바꾼다.")
    @Test
    void resolve() {
        // given
        Review review = saveReview();
        Comment comment =
                commentRepository.save(Comment.create(review.getId(), MEMBER_ID, "의견", null, null, null, MEMBER_ID));

        // when
        var result = commentService.resolve(new ResolveCommentCommand(comment.getId(), MEMBER_ID, true));

        // then
        assertThat(result.resolved()).isTrue();
    }

    @DisplayName("다른 리뷰의 코멘트를 상위 코멘트로 지정하면 예외가 발생한다.")
    @Test
    void addComment_invalidParent() {
        // given
        Review review = saveReview();
        Review otherReview =
                reviewRepository.save(Review.submit(review.getReviewRequestId(), 3L, 0, ReviewVerdict.APPROVED, 3L));
        Comment parent = commentRepository.save(
                Comment.create(otherReview.getId(), MEMBER_ID, "다른 리뷰", null, null, null, MEMBER_ID));

        // when & then
        assertThatThrownBy(() -> commentService.add(
                        new AddCommentCommand(review.getId(), MEMBER_ID, "잘못된 답글", null, null, parent.getId())))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_COMMENT_PARENT));
    }

    private Review saveReview() {
        Workspace workspace = workspaceRepository.save(Workspace.create("개발팀", 1L));
        participantRepository.save(Participant.owner(workspace.getId(), 1L));
        participantRepository.save(Participant.join(workspace.getId(), MEMBER_ID, Permission.REGULAR, 1L));
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(workspace.getId(), ReviewRequestType.DOCUMENT, "리뷰 요청", null, 1L, 1L));
        return reviewRepository.save(Review.submit(request.getId(), MEMBER_ID, 0, ReviewVerdict.APPROVED, MEMBER_ID));
    }
}
