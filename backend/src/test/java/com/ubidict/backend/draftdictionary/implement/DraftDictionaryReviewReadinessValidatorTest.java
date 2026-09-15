package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 준비 조건이 「후보어 판정 상태」에서 「전체 후보어의 유효성」으로 바뀐 것을 고정한다
 * ({@code docs/plan/DRAFT_PLAN.md}).
 *
 * <p>가장 중요한 두 가지는 <b>판정하지 않아도 통과한다</b>는 것과 <b>판정하지 않은 후보어도 등재 대상에 든다</b>는 것이다 — 예전 규칙이 남아 있으면
 * 초안 화면에서 판정을 걷어낸 뒤 리뷰 요청이 영구히 막힌다.
 */
@ExtendWith(MockitoExtension.class)
class DraftDictionaryReviewReadinessValidatorTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private DictionaryTermQueryPort dictionaryTermQueryPort;

    @InjectMocks
    private DraftDictionaryReviewReadinessValidator validator;

    @DisplayName("판정하지 않은 후보어만 있어도 대표어와 정의가 있으면 교정을 완료할 수 있다.")
    @Test
    void validateExamineCompletion_pendingButFilled() {
        DraftDictionary draft = examiningDraft();
        CandidateTerm pending = extracted("신규어", "신규 정의");

        assertThatCode(() -> validator.validateExamineCompletion(draft, List.of(pending)))
                .doesNotThrowAnyException();
    }

    @DisplayName("정의가 빈 후보어가 있으면 교정을 완료할 수 없다.")
    @Test
    void validateExamineCompletion_blankDefinition() {
        DraftDictionary draft = examiningDraft();
        CandidateTerm blank = extracted("신규어", null);

        assertThatThrownBy(() -> validator.validateExamineCompletion(draft, List.of(blank)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED);
    }

    @DisplayName("이미 교정 완료된 초안은 완료 재요청 오류로 구분한다.")
    @Test
    void validateExamineCompletion_alreadyExamined() {
        DraftDictionary draft = examinedDraft();

        assertThatThrownBy(() -> validator.validateExamineCompletion(draft, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXAMINED);
    }

    @DisplayName("첫 회차에서 등재할 용어가 없으면 리뷰를 요청할 수 없다.")
    @Test
    void validateReviewRequest_firstRoundWithoutTerm() {
        DraftDictionary draft = examinedDraft();
        given(dictionaryTermQueryPort.readActiveTerms(WORKSPACE_ID)).willReturn(List.of());

        assertThatThrownBy(() -> validator.validateReviewRequest(draft, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
    }

    @DisplayName("판정하지 않은 후보어도 등재 대상이므로 리뷰를 요청할 수 있다.")
    @Test
    void validateReviewRequest_pendingCandidateCountsAsFinalTerm() {
        DraftDictionary draft = examinedDraft();
        CandidateTerm pending = extracted("신규어", "신규 정의");
        given(dictionaryTermQueryPort.readActiveTerms(WORKSPACE_ID)).willReturn(List.of());

        assertThatCode(() -> validator.validateReviewRequest(draft, List.of(pending)))
                .doesNotThrowAnyException();
    }

    @DisplayName("승계 용어 집합이 활성 사전과 같으면 리뷰를 요청할 수 없다.")
    @Test
    void validateReviewRequest_unchangedExistingTerm() {
        DraftDictionary draft = examinedDraft();
        CandidateTerm existing = existing("기존어", "기존 정의", "existing");
        given(dictionaryTermQueryPort.readActiveTerms(WORKSPACE_ID))
                .willReturn(List.of(new TermSnapshot(10L, "기존어", "existing", "기존 정의")));

        assertThatThrownBy(() -> validator.validateReviewRequest(draft, List.of(existing)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
    }

    @DisplayName("승계 용어의 값이 바뀌면 리뷰를 요청할 수 있다.")
    @Test
    void validateReviewRequest_editedExistingTerm() {
        DraftDictionary draft = examinedDraft();
        CandidateTerm existing = existing("기존어", "기존 정의", "existing");
        existing.edit(null, "수정 정의", null);
        given(dictionaryTermQueryPort.readActiveTerms(WORKSPACE_ID))
                .willReturn(List.of(new TermSnapshot(10L, "기존어", "existing", "기존 정의")));

        assertThatCode(() -> validator.validateReviewRequest(draft, List.of(existing)))
                .doesNotThrowAnyException();
    }

    @DisplayName("활성 사전의 용어가 초안에서 빠지면 리뷰를 요청할 수 있다.")
    @Test
    void validateReviewRequest_removedExistingTerm() {
        DraftDictionary draft = examinedDraft();
        given(dictionaryTermQueryPort.readActiveTerms(WORKSPACE_ID))
                .willReturn(List.of(new TermSnapshot(10L, "기존어", "existing", "기존 정의")));

        assertThatCode(() -> validator.validateReviewRequest(draft, List.of())).doesNotThrowAnyException();
    }

    @DisplayName("정의가 빈 후보어가 있으면 리뷰를 요청할 수 없다.")
    @Test
    void validateReviewRequest_blankDefinition() {
        DraftDictionary draft = examinedDraft();
        CandidateTerm existing = existing("기존어", "기존 정의", "existing");
        existing.edit(null, "", null);

        assertThatThrownBy(() -> validator.validateReviewRequest(draft, List.of(existing)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED);
    }

    private static DraftDictionary examiningDraft() {
        return DraftDictionary.create(WORKSPACE_ID, null, List.of(100L), 2L);
    }

    private static DraftDictionary examinedDraft() {
        DraftDictionary draft = examiningDraft();
        draft.markExamined();
        return draft;
    }

    private static CandidateTerm extracted(String form, String definition) {
        return CandidateTerm.create(1L, form, definition, null, List.of(100L), 1, List.of("문맥"), 2L);
    }

    private static CandidateTerm existing(String form, String definition, String englishName) {
        return CandidateTerm.createExisting(1L, 10L, form, definition, englishName, 2L);
    }
}
