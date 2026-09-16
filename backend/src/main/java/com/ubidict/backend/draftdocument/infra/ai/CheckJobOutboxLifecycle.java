package com.ubidict.backend.draftdocument.infra.ai;

import com.ubidict.backend.common.infra.ai.LlmJobDispatchLifecycle;
import com.ubidict.backend.common.infra.ai.LlmJobFailureHandler;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobOutboxLifecycle implements LlmJobDispatchLifecycle, LlmJobFailureHandler {

    private final DraftDocumentCheckExecutionService executionService;

    @Override
    public boolean supports(LlmJobType jobType) {
        return jobType == LlmJobType.DOCUMENT_CHECK;
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
