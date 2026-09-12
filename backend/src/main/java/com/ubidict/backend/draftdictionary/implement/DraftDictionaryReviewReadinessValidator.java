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

@Component
@RequiredArgsConstructor
public class DraftDictionaryReviewReadinessValidator {
    private final DictionaryTermQueryPort dictionaryTermQueryPort;

    public void validateExamineCompletion(DraftDictionary draft, List<CandidateTerm> terms) {
        draft.validateExaminingForCompletion();
        if (terms.stream().anyMatch(CandidateTerm::isPending)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_UNDECIDED_EXISTS);
        }
    }

    public void validateReviewRequest(DraftDictionary draft, List<CandidateTerm> terms) {
        draft.validateExaminedForReview();
        List<TermSnapshot> finalTerms = terms.stream()
                .filter(CandidateTerm::isPublished)
                .map(DraftDictionaryReviewReadinessValidator::toSnapshot)
                .toList();
        if (finalTerms.stream()
                .anyMatch(term -> term.definition() == null || term.definition().isBlank())) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED);
        }

        Set<TermValue> finalValues = toValues(finalTerms);
        Set<TermValue> activeValues = toValues(dictionaryTermQueryPort.readActiveTerms(draft.getWorkspaceId()));
        if (finalValues.equals(activeValues)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
        }
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
