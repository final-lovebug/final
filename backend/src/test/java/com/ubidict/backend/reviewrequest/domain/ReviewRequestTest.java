package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.fixture.ReviewRequestFixture;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewRequestTest {

    private static final Long REQUESTER_ID = 1L;

    @DisplayName("리뷰 요청을 생성하면 리뷰대기 상태로 시작한다.")
    @Test
    void create() {
        // when
        ReviewRequest reviewRequest = ReviewRequest.create(
                10L, ReviewRequestType.DOCUMENT, "결제 문서 리뷰", "개정안을 검토합니다.", REQUESTER_ID, REQUESTER_ID);

        // then
        assertThat(reviewRequest.getStatus()).isEqualTo(ReviewRequestStatus.PENDING_REVIEW);
        assertThat(reviewRequest.getRequesterId()).isEqualTo(REQUESTER_ID);
    }

    @DisplayName("요청자는 리뷰 요청을 취소할 수 있다.")
    @Test
    void cancel() {
        // given
        ReviewRequest reviewRequest =
                ReviewRequestFixture.reviewRequest().requesterId(REQUESTER_ID).build();

        // when
        reviewRequest.cancel(REQUESTER_ID);

        // then
        assertThat(reviewRequest.getStatus()).isEqualTo(ReviewRequestStatus.CANCELED);
    }

    @DisplayName("반영된 리뷰 요청은 취소할 수 없다.")
    @Test
    void cancel_afterRevised() {
        // given
        ReviewRequest reviewRequest = ReviewRequestFixture.reviewRequest()
                .requesterId(REQUESTER_ID)
                .status(ReviewRequestStatus.REVISED)
                .build();

        // when & then
        assertThatThrownBy(() -> reviewRequest.cancel(REQUESTER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_STATUS_TRANSITION);
    }

    @DisplayName("리뷰 요청 제목이 비어 있으면 예외가 발생한다.")
    @Test
    void create_titleIsBlank() {
        // when & then
        assertThatThrownBy(() ->
                        ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "  ", null, REQUESTER_ID, REQUESTER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_TITLE_REQUIRED);
    }

    @DisplayName("리뷰 요청 상태는 반려 없이 여섯 개다.")
    @Test
    void status_hasSixValues() {
        assertThat(Arrays.asList(ReviewRequestStatus.values()))
                .containsExactly(
                        ReviewRequestStatus.PENDING_REVIEW,
                        ReviewRequestStatus.IN_REVIEW,
                        ReviewRequestStatus.CHANGES_REQUESTED,
                        ReviewRequestStatus.APPROVED,
                        ReviewRequestStatus.REVISED,
                        ReviewRequestStatus.CANCELED);
    }
}
