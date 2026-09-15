package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobCreationPolicyValidator {

    private static final List<ExtractionJobStatus> IN_PROGRESS =
            List.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING);

    private final ExtractionJobRepository extractionJobRepository;

    public void validate(Long workspaceId) {
        if (extractionJobRepository.existsByWorkspaceIdAndStatusInAndDeletedAtIsNull(workspaceId, IN_PROGRESS)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_ALREADY_RUNNING);
        }
    }
}
