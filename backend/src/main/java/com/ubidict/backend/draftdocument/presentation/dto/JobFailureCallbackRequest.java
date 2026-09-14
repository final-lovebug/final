package com.ubidict.backend.draftdocument.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워커가 보내는 대조 실패 콜백.
 *
 * @param reason 사용자에게 그대로 보이는 사유. 작업 행의 {@code failureReason}에 담긴다
 * @param code 워커 내부의 실패 분류. <b>저장하지 않고 로그에만 남긴다</b>
 */
public record JobFailureCallbackRequest(
        @NotBlank String requestId, @Size(max = 1000) String reason, String code) {}
