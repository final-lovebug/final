package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import java.util.Set;

public record CandidateTermSearchQuery(
        Long draftDictionaryId,
        CandidateTermStatus status,
        String form,
        Integer minOccurrenceCount,
        int page,
        int size,
        String sort) {
    public CandidateTermSearchQuery {
        if (draftDictionaryId == null
                || page < 0
                || size < 1
                || size > 100
                || (minOccurrenceCount != null && minOccurrenceCount < 0)) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = sort == null ? "occurrenceCount,desc" : sort;
        String[] parts = sort.split(",", -1);
        if (parts.length != 2
                || !Set.of("occurrenceCount", "form", "createdAt", "updatedAt", "id")
                        .contains(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = parts[0] + "," + parts[1].toLowerCase();
    }
}
