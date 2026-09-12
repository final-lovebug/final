package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CandidateTermDecisionTest {

    @DisplayName("후보어를 등재 승인하면 처리자를 기록한다.")
    @Test
    void approveRegistration() {
        // given
        CandidateTerm candidate = candidate("정의");

        // when
        candidate.approveRegistration(7L);

        // then
        assertThat(candidate.getStatus()).isEqualTo(CandidateTermStatus.REGISTRATION_APPROVED);
        assertThat(candidate.getHandledBy()).isEqualTo(7L);
    }

    @DisplayName("정의가 비어 있으면 후보어를 등재 승인할 수 없다.")
    @Test
    void approveRegistration_definitionIsBlank() {
        // given
        CandidateTerm candidate = candidate(" ");

        // when & then
        assertThatThrownBy(() -> candidate.approveRegistration(7L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED));
    }

    @DisplayName("편입 대상이 없으면 동의어로 편입할 수 없다.")
    @Test
    void mergeAsSynonym_targetIsNull() {
        // given
        CandidateTerm candidate = candidate("정의");

        // when & then
        assertThatThrownBy(() -> candidate.mergeAsSynonym(7L, null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_MERGE_TARGET_REQUIRED));
    }

    @DisplayName("거절 사유가 비어 있으면 후보어를 거절할 수 없다.")
    @Test
    void reject_reasonIsBlank() {
        // given
        CandidateTerm candidate = candidate("정의");

        // when & then
        assertThatThrownBy(() -> candidate.reject(7L, " "))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_REJECT_REASON_REQUIRED));
    }

    @DisplayName("후보어를 보류하면 처리자를 기록한다.")
    @Test
    void hold() {
        // given
        CandidateTerm candidate = candidate("정의");

        // when
        candidate.hold(7L);

        // then
        assertThat(candidate.getStatus()).isEqualTo(CandidateTermStatus.ON_HOLD);
        assertThat(candidate.getHandledBy()).isEqualTo(7L);
    }

    @DisplayName("교정 중이면 거절한 후보어를 다시 승인할 수 있다.")
    @Test
    void approveRegistration_afterReject() {
        // given
        CandidateTerm candidate = candidate("정의");
        candidate.reject(7L, "중복 용어");

        // when
        candidate.approveRegistration(8L);

        // then
        assertThat(candidate.getStatus()).isEqualTo(CandidateTermStatus.REGISTRATION_APPROVED);
        assertThat(candidate.getHandledBy()).isEqualTo(8L);
        assertThat(candidate.getRejectReason()).isNull();
        assertThat(candidate.getMergeTargetTermId()).isNull();
    }

    private CandidateTerm candidate(String definition) {
        return CandidateTerm.create(1L, "후보어", definition, null, List.of(10L), 1, List.of("문맥"), 2L);
    }
}
