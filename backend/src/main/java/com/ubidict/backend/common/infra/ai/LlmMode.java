package com.ubidict.backend.common.infra.ai;

/**
 * 워커가 실제 LLM을 호출할지 정하는 축(D-71).
 *
 * <p>{@code STUB}은 기존 빈 결과 대역, {@code MOCK}은 개발용 고정 목 결과, {@code REAL}은 실제 모델 호출이다. 값은 요청 메시지에
 * 실려 나가므로 <b>백엔드가 아니라 워커가 이 분기를 수행한다.</b>
 */
public enum LlmMode {
    STUB,
    MOCK,
    REAL
}
