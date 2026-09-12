package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CandidateTermTest {

    @DisplayName("후보어를 생성하면 대기 상태로 시작한다.")
    @Test
    void create() {
        // when
        CandidateTerm candidate = CandidateTerm.create(1L, "term", null, null, List.of(), 1, List.of());

        // then
        assertThat(candidate.getStatus()).isEqualTo(CandidateTermStatus.PENDING);
    }

    @DisplayName("표기가 비어 있으면 후보어를 생성할 수 없다.")
    @Test
    void create_formIsBlank() {
        // when & then
        assertThatThrownBy(() -> CandidateTerm.create(1L, " ", null, null, List.of(), 1, List.of()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_FORM));
    }

    @DisplayName("후보어의 제안 정의를 수정한다.")
    @Test
    void edit() {
        // given
        CandidateTerm candidate = CandidateTerm.create(1L, "term", "definition", null, List.of(), 1, List.of());

        // when
        candidate.edit(null, "updated", null);

        // then
        assertThat(candidate.getProposedDefinition()).isEqualTo("updated");
    }
}
