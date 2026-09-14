package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviseTest {

    @DisplayName("반영 결과 버전과 수행자를 이력으로 기록한다.")
    @Test
    void perform() {
        // when
        Revise revise = Revise.perform(1L, 2, 3L);

        // then
        assertThat(revise.getReviewRequestId()).isEqualTo(1L);
        assertThat(revise.getResultVersionNo()).isEqualTo(2);
        assertThat(revise.getPerformedBy()).isEqualTo(3L);
        assertThat(revise.getPerformedAt()).isNotNull();
    }

    @DisplayName("결과 버전이 1보다 작으면 반영 이력을 만들 수 없다.")
    @Test
    void perform_resultVersionIsInvalid() {
        // when & then
        assertThatThrownBy(() -> Revise.perform(1L, 0, 3L)).isInstanceOf(BusinessException.class);
    }
}
