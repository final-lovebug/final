package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.service.model.BulkDecideCandidateTermsCommand;
import com.ubidict.backend.draftdictionary.service.model.BulkDecisionResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandidateTermBulkDecisionProcessor {
    private final CandidateTermDecisionProcessor processor;

    public BulkDecisionResult decide(
            DraftDictionary draft, List<CandidateTerm> terms, BulkDecideCandidateTermsCommand command) {
        draft.validateExamining();
        List<Long> succeeded = new ArrayList<>();
        List<BulkDecisionResult.Failure> failed = new ArrayList<>();
        Map<Long, CandidateTerm> termsById =
                terms.stream().collect(LinkedHashMap::new, (items, term) -> items.put(term.getId(), term), Map::putAll);

        for (Long id : command.candidateTermIds()) {
            CandidateTerm term = termsById.get(id);
            if (term == null) {
                failed.add(new BulkDecisionResult.Failure(
                        id,
                        DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_FOUND.code(),
                        DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_FOUND.message()));
                continue;
            }
            try {
                processor.decide(draft, term, command.toSingle(id));
                succeeded.add(id);
            } catch (BusinessException e) {
                failed.add(new BulkDecisionResult.Failure(id, e.errorCode().code(), e.getMessage()));
            }
        }
        return new BulkDecisionResult(succeeded, failed);
    }
}
