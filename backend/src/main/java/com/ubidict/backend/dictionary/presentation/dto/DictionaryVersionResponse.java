package com.ubidict.backend.dictionary.presentation.dto;

import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import java.time.OffsetDateTime;

/**
 * 버전 이력 한 줄. 버전이 쌓였을 때 응답이 비대해지지 않도록 용어를 싣지 않고 개수만 담는다.
 */
public record DictionaryVersionResponse(
        Long dictionaryId,
        int versionNo,
        DictionaryStatus status,
        OffsetDateTime publishedAt,
        Long createdBy,
        long termCount) {

    public static DictionaryVersionResponse from(DictionaryVersionResult result) {
        return new DictionaryVersionResponse(
                result.dictionaryId(),
                result.versionNo(),
                result.status(),
                result.publishedAt(),
                result.createdBy(),
                result.termCount());
    }
}
