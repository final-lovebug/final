package com.ubidict.backend.revisionlog.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevisionSummaryFactoryTest {

    private final DictionaryDiffCalculator calculator = new DictionaryDiffCalculator();
    private final RevisionSummaryFactory factory = new RevisionSummaryFactory();

    @DisplayName("첫 사전집 버전은 최초 발행 요약을 만든다.")
    @Test
    void forDictionary_initial() {
        // when
        String summary = factory.forDictionary(RevisionLogGrade.INITIAL, calculator.calculate(List.of(), List.of()));

        // then
        assertThat(summary).isEqualTo("사전집 최초 발행");
    }

    @DisplayName("추가된 용어 수로 사전집 요약을 만든다.")
    @Test
    void forDictionary_newTerms() {
        // when
        String summary = factory.forDictionary(
                RevisionLogGrade.NEW_TERMS,
                calculator.calculate(List.of(), List.of(new TermSnapshot("사용자", null, "정의"))));

        // then
        assertThat(summary).isEqualTo("용어 1개 추가");
    }
}
