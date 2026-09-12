package com.ubidict.backend.dictionary.presentation.dto;

import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import java.time.OffsetDateTime;
import java.util.List;

public record DictionaryResponse(
        Long dictionaryId,
        Long workspaceId,
        int versionNo,
        DictionaryStatus status,
        OffsetDateTime publishedAt,
        Long createdBy,
        List<TermResponse> terms) {

    public static DictionaryResponse from(DictionaryResult result) {
        return new DictionaryResponse(
                result.dictionaryId(),
                result.workspaceId(),
                result.versionNo(),
                result.status(),
                result.publishedAt(),
                result.createdBy(),
                result.terms().stream().map(TermResponse::from).toList());
    }
}
