package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import org.junit.jupiter.api.Test;

class ReviewerDuplicationValidatorTest {
    @Test
    void validate_duplicate() {
        var repo = mock(ReviewerRepository.class);
        when(repo.existsByReviewRequestIdAndMemberId(1L, 2L)).thenReturn(true);
        var v = new ReviewerDuplicationValidator(repo);
        assertThatThrownBy(() -> v.validate(1L, 2L))
                .isInstanceOf(com.ubidict.backend.common.exception.BusinessException.class)
                .extracting(e -> ((com.ubidict.backend.common.exception.BusinessException) e).errorCode())
                .isEqualTo(
                        com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode
                                .REVIEW_REQUEST_DUPLICATE_REVIEWER);
    }
}
