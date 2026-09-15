package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewerDuplicationValidatorTest {

    @DisplayName("같은 회원을 리뷰어로 중복 지정하면 예외가 발생한다.")
    @Test
    void validate_duplicate() {
        // given
        ReviewerRepository repository = mock(ReviewerRepository.class);
        given(repository.existsByReviewRequestIdAndMemberId(1L, 2L)).willReturn(true);
        ReviewerDuplicationValidator validator = new ReviewerDuplicationValidator(repository);

        // when & then
        assertThatThrownBy(() -> validator.validate(1L, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_DUPLICATE_REVIEWER));
    }
}
