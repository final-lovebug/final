package com.ubidict.backend.common.infra.ai;

/** DLQ 수신이 Job 도메인에 실패 보상을 위임하는 공통 포트. */
public interface LlmJobFailureHandler {

    boolean supports(LlmJobType jobType);

    void fail(Long jobId, String requestId, String reason);
}
