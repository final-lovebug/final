package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.infra.port.DraftDocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDictionaryCreationPolicyValidator {

    private final DraftDictionaryRepository draftDictionaryRepository;
    private final DraftDocumentQueryPort draftDocumentQueryPort;

    public void validate(Long workspaceId) {
        if (draftDictionaryRepository.existsByWorkspaceIdAndStatusNotAndDeletedAtIsNull(
                        workspaceId, DraftDictionaryStatus.REVISED)
                || draftDocumentQueryPort.hasOngoingDraft(workspaceId)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXISTS);
        }
    }
}
