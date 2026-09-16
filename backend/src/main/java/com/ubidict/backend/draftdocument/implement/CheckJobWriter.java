package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobWriter {

    private final CheckJobRepository checkJobRepository;

    /**
     * <b>{@code saveAndFlush}인 이유</b> — 「문서당 진행 중 작업 1개」의 최종 판정자는
     * {@code uk_check_job_document_in_progress}다. 지연 flush로 두면 제약 위반이 커밋 시점에 터져
     * {@code DataIntegrityViolationException}이 그대로 500으로 나간다. 여기서 즉시 터뜨려 사전 검사와 같은 409로 맞춘다.
     */
    public CheckJob append(Long documentId, Long requestedBy) {
        try {
            return checkJobRepository.saveAndFlush(CheckJob.create(documentId, requestedBy));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_ALREADY_RUNNING,
                    "동시 요청으로 진행 중인 대조 작업이 이미 있습니다. documentId=" + documentId,
                    exception);
        }
    }
}
