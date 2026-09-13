package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReexamineTest {

    @DisplayName("최초 재교정 회차는 1이다.")
    @Test
    void perform() {
        // when
        Reexamine reexamine = Reexamine.perform(1L, 1, 2L, List.of(3L, 4L));

        // then
        assertThat(reexamine.getRound()).isEqualTo(1);
        assertThat(reexamine.getAddressedCommentIds()).containsExactly(3L, 4L);
        assertThat(reexamine.getPerformedAt()).isNotNull();
    }

    @DisplayName("재교정 회차가 0이면 생성할 수 없다.")
    @Test
    void perform_roundIsZero() {
        // when & then
        assertThatThrownBy(() -> Reexamine.perform(1L, 0, 2L, List.of())).isInstanceOf(BusinessException.class);
    }
}
