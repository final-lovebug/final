package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobWriter {

    private final ExtractionJobRepository extractionJobRepository;

    /**
     * <b>{@code saveAndFlush}인 이유</b> — 「워크스페이스당 진행 중 작업 1개」의 최종 판정자는
     * {@code uk_extraction_job_workspace_in_progress}다. 지연 flush로 두면 제약 위반이 커밋 시점에 터져
     * {@code DataIntegrityViolationException}이 그대로 500으로 나간다. 여기서 즉시 터뜨려 사전 검사와 같은 409로 맞춘다.
     */
    public ExtractionJob append(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        try {
            return extractionJobRepository.saveAndFlush(
                    ExtractionJob.create(workspaceId, dictionaryId, sourceDocumentIds, requestedBy));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_ALREADY_RUNNING,
                    "동시 요청으로 진행 중인 추출 작업이 이미 있습니다. workspaceId=" + workspaceId,
                    exception);
        }
    }
}
