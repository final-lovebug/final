package com.ubidict.backend.draftdocument.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermCheckerStubTest {

    private final TermCheckerStub termCheckerStub = new TermCheckerStub();

    @DisplayName("대조기 스텁은 빈 고정 결과를 반환한다.")
    @Test
    void check() {
        assertThat(termCheckerStub.check(new DocumentSnapshot(1L, 2L, 1, "본문"), List.of()))
                .isEmpty();
    }
}
