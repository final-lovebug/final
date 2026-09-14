package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CandidateTermOwnershipValidator {
    public void validate(CandidateTerm term, Long draftDictionaryId) {
        if (!term.getDraftDictionaryId().equals(draftDictionaryId))
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_MISMATCHED);
    }
}
