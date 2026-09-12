package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import org.springframework.stereotype.Component;

@Component
public class CandidateTermDecisionProcessor {
    public CandidateTerm decide(DraftDictionary draft, CandidateTerm term, DecideCandidateTermCommand command) {
        draft.validateExamining();
        switch (command.decision()) {
            case REGISTRATION_APPROVED -> term.approveRegistration(command.memberId());
            case MERGED_AS_SYNONYM -> term.mergeAsSynonym(command.memberId(), command.mergeTargetTermId());
            case REJECTED -> term.reject(command.memberId(), command.rejectReason());
            case ON_HOLD -> term.hold(command.memberId());
            default -> throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_DECISION);
        }
        return term;
    }
}
