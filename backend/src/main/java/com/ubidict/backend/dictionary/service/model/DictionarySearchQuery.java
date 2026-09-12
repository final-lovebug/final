package com.ubidict.backend.dictionary.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import java.util.Set;

public record DictionarySearchQuery(int page, int size, String sort, String keyword) {

    public DictionarySearchQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = sort == null ? "preferredForm,asc" : sort;
        String[] parts = sort.split(",", -1);
        if (parts.length != 2
                || !Set.of("preferredForm", "createdAt").contains(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = parts[0] + "," + parts[1].toLowerCase();
    }

    public String sortField() {
        return sort.split(",", -1)[0];
    }

    public boolean ascending() {
        return sort.endsWith(",asc");
    }
}
