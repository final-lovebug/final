package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobReader {

    private final CheckJobRepository checkJobRepository;

    public CheckJob read(Long checkJobId) {
        return checkJobRepository
                .findByIdAndDeletedAtIsNull(checkJobId)
                .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_NOT_FOUND));
    }
}
