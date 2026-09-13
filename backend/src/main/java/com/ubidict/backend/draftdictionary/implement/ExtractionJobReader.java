package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobReader {

    private final ExtractionJobRepository extractionJobRepository;

    public ExtractionJob read(Long extractionJobId) {
        return extractionJobRepository
                .findByIdAndDeletedAtIsNull(extractionJobId)
                .orElseThrow(
                        () -> new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_NOT_FOUND));
    }
}
