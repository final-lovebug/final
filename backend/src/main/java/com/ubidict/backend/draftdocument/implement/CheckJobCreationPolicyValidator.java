package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobCreationPolicyValidator {

    private static final List<CheckJobStatus> IN_PROGRESS = List.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING);

    private final CheckJobRepository checkJobRepository;

    public void validate(Long documentId) {
        if (checkJobRepository.existsByDocumentIdAndStatusInAndDeletedAtIsNull(documentId, IN_PROGRESS)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_ALREADY_RUNNING);
        }
    }
}
