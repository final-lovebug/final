package com.ubidict.backend.common.infra.ai;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class LlmJobOutboxService {

    private final LlmJobOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void enqueue(LlmJobRequest request) {
        try {
            repository.save(LlmJobOutbox.create(request, objectMapper.writeValueAsString(request)));
        } catch (JacksonException exception) {
            throw new BusinessException(
                    MessagingErrorCode.MESSAGING_LLM_REQUEST_PUBLISH_FAILED,
                    "AI 작업 요청 직렬화 실패. jobType=" + request.jobType() + ", jobId=" + request.jobId(),
                    exception);
        }
    }

    public void cancel(String requestId) {
        repository.findByRequestId(requestId).ifPresent(LlmJobOutbox::cancel);
    }
}
