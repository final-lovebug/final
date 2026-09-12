package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.service.model.BulkDecideCandidateTermsCommand;
import com.ubidict.backend.draftdictionary.service.model.BulkDecisionResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CandidateTermBulkDecisionProcessorTest {

    private final CandidateTermBulkDecisionProcessor processor =
            new CandidateTermBulkDecisionProcessor(new CandidateTermDecisionProcessor());

    @DisplayName("일괄 판정은 성공과 실패를 후보어별로 나누어 반환한다.")
    @Test
    void decide_partialSuccess() {
        DraftDictionary draft = draft();
        CandidateTerm candidate = candidate(1L);
        BulkDecideCandidateTermsCommand command = new BulkDecideCandidateTermsCommand(
                1L, List.of(1L, 999L), 2L, CandidateTermStatus.REGISTRATION_APPROVED, null, null);

        BulkDecisionResult result = processor.decide(draft, List.of(candidate), command);

        assertThat(result.succeeded()).containsExactly(1L);
        assertThat(result.failed()).hasSize(1);
        assertThat(result.failed().getFirst().candidateTermId()).isEqualTo(999L);
        assertThat(result.failed().getFirst().code())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_FOUND.code());
        assertThat(candidate.getStatus()).isEqualTo(CandidateTermStatus.REGISTRATION_APPROVED);
    }

    @DisplayName("일괄 판정에서도 이미 판정된 후보어를 다시 판정할 수 있다.")
    @Test
    void decide_redecision() {
        DraftDictionary draft = draft();
        CandidateTerm candidate = candidate(1L);
        candidate.reject(3L, "오등록");
        BulkDecideCandidateTermsCommand command = new BulkDecideCandidateTermsCommand(
                1L, List.of(1L), 2L, CandidateTermStatus.REGISTRATION_APPROVED, null, null);

        BulkDecisionResult result = processor.decide(draft, List.of(candidate), command);

        assertThat(result.succeeded()).containsExactly(1L);
        assertThat(result.failed()).isEmpty();
        assertThat(candidate.getStatus()).isEqualTo(CandidateTermStatus.REGISTRATION_APPROVED);
        assertThat(candidate.getHandledBy()).isEqualTo(2L);
        assertThat(candidate.getRejectReason()).isNull();
    }

    @DisplayName("교정 완료된 초안은 일괄 판정을 시작할 수 없다.")
    @Test
    void decide_examinedDraft() {
        DraftDictionary draft = draft();
        draft.markExamined();
        BulkDecideCandidateTermsCommand command =
                new BulkDecideCandidateTermsCommand(1L, List.of(1L), 2L, CandidateTermStatus.ON_HOLD, null, null);

        assertThatThrownBy(() -> processor.decide(draft, List.of(candidate(1L)), command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINABLE);
    }

    private static DraftDictionary draft() {
        return DraftDictionary.create(1L, null, List.of(100L), 2L);
    }

    private static CandidateTerm candidate(Long id) {
        CandidateTerm candidate = CandidateTerm.create(1L, "신규어", "신규 정의", null, List.of(100L), 1, List.of("문맥"), 2L);
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }
}
