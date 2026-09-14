package com.ubidict.backend.revisionlog.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DictionaryDiffCalculatorTest {

    private final DictionaryDiffCalculator calculator = new DictionaryDiffCalculator();

    @DisplayName("사전집 용어의 추가, 변경, 삭제를 구분한다.")
    @Test
    void calculate() {
        // given
        List<TermSnapshot> previous = List.of(
                new TermSnapshot("사용자", "User", "서비스 이용자"),
                new TermSnapshot("구독", "Subscription", "서비스 이용 계약"),
                new TermSnapshot("이용자", "User", "기존 표기"));
        List<TermSnapshot> current =
                List.of(new TermSnapshot("사용자", "User", "서비스를 이용하는 사람"), new TermSnapshot("결제", "Payment", "대금 지불"));

        // when
        DictionaryDiff diff = calculator.calculate(previous, current);

        // then
        assertThat(diff.addedCount()).isEqualTo(1);
        assertThat(diff.changedCount()).isEqualTo(1);
        assertThat(diff.removedCount()).isEqualTo(2);
        assertThat(diff.changes())
                .extracting(change -> change.changeType())
                .containsExactly(
                        RevisionLogChangeType.CHANGED,
                        RevisionLogChangeType.ADDED,
                        RevisionLogChangeType.REMOVED,
                        RevisionLogChangeType.REMOVED);
    }

    @DisplayName("대표어가 바뀌면 삭제와 추가로 기록한다.")
    @Test
    void calculate_renamedPreferredForm() {
        // when
        DictionaryDiff diff = calculator.calculate(
                List.of(new TermSnapshot("이용자", "User", "서비스 이용자")),
                List.of(new TermSnapshot("사용자", "User", "서비스 이용자")));

        // then
        assertThat(diff.changes())
                .extracting(change -> change.changeType())
                .containsExactly(RevisionLogChangeType.ADDED, RevisionLogChangeType.REMOVED);
    }

    @DisplayName("정의만 달라도 용어 변경으로 기록한다.")
    @Test
    void calculate_definitionChanged() {
        // when
        DictionaryDiff diff = calculator.calculate(
                List.of(new TermSnapshot("사용자", "User", "기존 정의")), List.of(new TermSnapshot("사용자", "User", "새 정의")));

        // then
        assertThat(diff.changes()).singleElement().satisfies(change -> {
            assertThat(change.changeType()).isEqualTo(RevisionLogChangeType.CHANGED);
            assertThat(change.detail()).isEqualTo("정의 수정");
        });
    }
}
