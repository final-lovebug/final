package com.ubidict.backend.draftdictionary.infra.ai;

import com.ubidict.backend.common.infra.ai.LlmJobDispatchLifecycle;
import com.ubidict.backend.common.infra.ai.LlmJobFailureHandler;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobOutboxLifecycle implements LlmJobDispatchLifecycle, LlmJobFailureHandler {

    private final DraftDictionaryExtractionExecutionService executionService;

    @Override
    public boolean supports(LlmJobType jobType) {
        return jobType == LlmJobType.TERM_EXTRACTION;
    }

    @Override
    public boolean prepare(LlmJobRequest request) {
        return executionService.prepareDispatch(request.jobId(), request.requestId());
    }

    @Override
    public void fail(Long jobId, String requestId, String reason) {
        executionService.fail(jobId, requestId, reason);
    }
}
