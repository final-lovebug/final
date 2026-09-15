package com.ubidict.backend.revisionlog.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DictionaryGradeDeciderTest {

    private final DictionaryDiffCalculator calculator = new DictionaryDiffCalculator();
    private final DictionaryGradeDecider decider = new DictionaryGradeDecider();

    @DisplayName("첫 사전집 버전은 최초 발행 등급이다.")
    @Test
    void decide_initial() {
        // when
        RevisionLogGrade grade = decider.decide(null, calculator.calculate(List.of(), List.of()));

        // then
        assertThat(grade).isEqualTo(RevisionLogGrade.INITIAL);
    }

    @DisplayName("삭제가 하나라도 있으면 재검사 등급이다.")
    @Test
    void decide_removedTerm() {
        // when
        RevisionLogGrade grade = decider.decide(
                1,
                calculator.calculate(
                        List.of(new TermSnapshot("이용자", null, "기존")), List.of(new TermSnapshot("사용자", null, "새 용어"))));

        // then
        assertThat(grade).isEqualTo(RevisionLogGrade.RECHECK_REQUIRED);
    }

    @DisplayName("추가만 있으면 새 지적 등급이다.")
    @Test
    void decide_addedTerm() {
        // when
        RevisionLogGrade grade =
                decider.decide(1, calculator.calculate(List.of(), List.of(new TermSnapshot("사용자", null, "정의"))));

        // then
        assertThat(grade).isEqualTo(RevisionLogGrade.NEW_TERMS);
    }

    @DisplayName("정의만 바뀌면 영향 없음 등급이다.")
    @Test
    void decide_changedTermOnly() {
        // when
        RevisionLogGrade grade = decider.decide(
                1,
                calculator.calculate(
                        List.of(new TermSnapshot("사용자", null, "기존 정의")),
                        List.of(new TermSnapshot("사용자", null, "새 정의"))));

        // then
        assertThat(grade).isEqualTo(RevisionLogGrade.NO_IMPACT);
    }
}
