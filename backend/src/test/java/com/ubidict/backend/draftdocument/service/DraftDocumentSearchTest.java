package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.*;

import com.ubidict.backend.common.exception.*;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentSearchQuery;
import org.junit.jupiter.api.Test;

class DraftDocumentSearchTest {
    @Test
    void query_acceptsWhitelist() {
        assertThat(new DraftDocumentSearchQuery(null, null, 0, 20, "updatedAt,asc").sort())
                .isEqualTo("updatedAt,asc");
    }

    @Test
    void query_rejectsUnknownSort() {
        assertThatThrownBy(() -> new DraftDocumentSearchQuery(null, null, 0, 20, "body,asc"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(CommonErrorCode.COMMON_INVALID_REQUEST);
    }
}
