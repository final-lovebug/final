package com.ubidict.backend.draftdictionary.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermExtractorStubTest {

    @DisplayName("용어 추출 스텁은 외부 호출 없이 빈 결과를 반환한다.")
    @Test
    void extract() {
        assertThat(new TermExtractorStub().extract(List.of(10L), List.of())).isEmpty();
    }
}
