package com.ubidict.backend.draftdictionary.implement;

import static com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_FOUND;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDictionaryReader {

    private final DraftDictionaryRepository draftDictionaryRepository;

    public DraftDictionary read(Long draftDictionaryId) {
        return draftDictionaryRepository
                .findByIdAndDeletedAtIsNull(draftDictionaryId)
                .orElseThrow(() -> new BusinessException(DRAFT_DICTIONARY_NOT_FOUND));
    }
}
