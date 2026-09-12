package com.ubidict.backend.draftdocument.service.model;

import com.ubidict.backend.common.exception.*;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;

public record SuggestionTermSearchQuery(
        Long draftDocumentId, SuggestionTermStatus status, int page, int size, String sort) {
    public SuggestionTermSearchQuery {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        String[] p = sort == null ? new String[] {"createdAt", "desc"} : sort.split(",");
        if (p.length != 2
                || !java.util.Set.of("createdAt", "updatedAt", "id").contains(p[0])
                || !(p[1].equalsIgnoreCase("asc") || p[1].equalsIgnoreCase("desc")))
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        sort = p[0] + "," + p[1].toLowerCase();
    }
}
