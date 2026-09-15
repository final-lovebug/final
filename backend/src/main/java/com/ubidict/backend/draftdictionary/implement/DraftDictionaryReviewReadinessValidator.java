package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 교정 완료·리뷰 요청의 준비 조건을 판정한다.
 *
 * <p><b>조건이 「후보어 판정 상태」에서 「전체 후보어의 유효성」으로 바뀌었다</b>({@code docs/plan/DRAFT_PLAN.md}). 초안 화면에서 승인·보류·거절·편입을
 * 걷어내면서, 승인 건수를 세는 방식으로는 준비 여부를 판단할 수 없게 됐다 — 모든 후보어가 {@code PENDING}으로 남기 때문이다.
 *
 * <p>이제 묻는 것은 둘뿐이다. <b>모든 후보어가 대표어와 정의를 가졌는가</b>, 그리고 <b>그 결과가 활성 사전집과 실제로 다른가</b>. 대표어는
 * {@code form}이 항상 채워져 있으므로(생성 시 필수) 실제로 막는 것은 대개 정의다.
 */
@Component
@RequiredArgsConstructor
public class DraftDictionaryReviewReadinessValidator {
    private final DictionaryTermQueryPort dictionaryTermQueryPort;

    /**
     * 교정 완료 조건.
     *
     * <p>예전에는 미판정({@code PENDING}) 후보어가 없는지 물었다. 판정 UI가 사라진 뒤에는 그 조건이 영구히 거짓이라 대신 유효성을 본다.
     */
    public void validateExamineCompletion(DraftDictionary draft, List<CandidateTerm> terms) {
        draft.validateExaminingForCompletion();
        validateAllTermsFilled(terms);
    }

    /**
     * 리뷰 요청 조건.
     *
     * <p><b>모든 후보어의 대표어가 차기 사전집 목록에 들어간다</b> — 예전처럼 {@code REGISTRATION_APPROVED}·{@code KEPT}만 고르지
     * 않는다. 초안에 남아 있다는 것이 곧 등재 의사이고, 빼고 싶은 후보어는 판정이 아니라 삭제로 뺀다.
     */
    public void validateReviewRequest(DraftDictionary draft, List<CandidateTerm> terms) {
        draft.validateExaminedForReview();
        validateAllTermsFilled(terms);

        Set<TermValue> nextValues = toValues(terms.stream()
                .map(DraftDictionaryReviewReadinessValidator::toSnapshot)
                .toList());
        Set<TermValue> activeValues = toValues(dictionaryTermQueryPort.readActiveTerms(draft.getWorkspaceId()));
        if (nextValues.equals(activeValues)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
        }
    }

    /** 대표어와 정의가 비어 있는 후보어가 하나라도 있으면 거절한다. */
    private void validateAllTermsFilled(List<CandidateTerm> terms) {
        if (terms.stream().anyMatch(term -> isBlank(term.getForm()))) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_FORM_REQUIRED);
        }
        if (terms.stream().anyMatch(term -> isBlank(term.getProposedDefinition()))) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static TermSnapshot toSnapshot(CandidateTerm term) {
        return new TermSnapshot(
                term.getSourceTermId(), term.getForm(), term.getProposedEnglishName(), term.getProposedDefinition());
    }

    private static Set<TermValue> toValues(List<TermSnapshot> terms) {
        return terms.stream()
                .map(term -> new TermValue(term.preferredForm(), term.englishName(), term.definition()))
                .collect(Collectors.toSet());
    }

    private record TermValue(String preferredForm, String englishName, String definition) {}
}
