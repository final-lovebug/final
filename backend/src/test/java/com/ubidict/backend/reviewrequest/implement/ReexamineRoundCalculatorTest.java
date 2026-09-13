package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReexamineRoundCalculatorTest {

    private final ReexamineRoundCalculator calculator = new ReexamineRoundCalculator();

    @DisplayName("현재 개정안 다음 번호를 재교정 회차로 계산한다.")
    @Test
    void nextRound() {
        // when
        int round = calculator.nextRound(2);

        // then
        assertThat(round).isEqualTo(3);
    }

    @DisplayName("최초 개정안 다음 재교정 회차는 1이다.")
    @Test
    void nextRound_firstReexamine() {
        // when
        int round = calculator.nextRound(0);

        // then
        assertThat(round).isEqualTo(1);
    }
}
