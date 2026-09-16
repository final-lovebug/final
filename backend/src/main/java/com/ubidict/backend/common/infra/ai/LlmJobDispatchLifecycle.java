package com.ubidict.backend.common.infra.ai;

/** Job 도메인이 SQS 전송 직전 상태 전이를 소유하도록 하는 공통 포트. */
public interface LlmJobDispatchLifecycle {

    boolean supports(LlmJobType jobType);

    /** 같은 requestId의 PENDING/RUNNING Job만 전송 가능하게 만든다. */
    boolean prepare(LlmJobRequest request);
}
