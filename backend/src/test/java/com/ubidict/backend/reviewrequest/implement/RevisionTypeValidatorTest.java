package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevisionTypeValidatorTest {

    private final RevisionTypeValidator validator = new RevisionTypeValidator();

    @DisplayName("문서 유형 요청에 문서 개정안을 등록할 수 있다.")
    @Test
    void validate() {
        // given
        ReviewRequest request = ReviewRequest.create(1L, ReviewRequestType.DOCUMENT, "문서 리뷰", null, 1L, 1L);

        // when & then
        assertThatCode(() -> validator.validate(request, true)).doesNotThrowAnyException();
    }

    @DisplayName("사전 유형 요청에 문서 개정안을 붙이면 예외가 발생한다.")
    @Test
    void validate_typeMismatched() {
        // given
        ReviewRequest request = ReviewRequest.create(1L, ReviewRequestType.DICTIONARY, "사전 리뷰", null, 1L, 1L);

        // when & then
        assertThatThrownBy(() -> validator.validate(request, true))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_TYPE_MISMATCHED));
    }
}
