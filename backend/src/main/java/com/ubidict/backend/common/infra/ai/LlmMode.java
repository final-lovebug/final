package com.ubidict.backend.common.infra.ai;

/**
 * 워커가 실제 LLM을 호출할지 정하는 축(D-71).
 *
 * <p>{@code STUB}이면 워커는 모델을 부르지 않고 임의 시간을 기다린 뒤 목 데이터를 돌려준다 — 비용 없이 전 구간 왕복을 확인하기 위한 것이다. 값은 요청 메시지에
 * 실려 나가므로 <b>백엔드가 아니라 워커가 이 분기를 수행한다.</b>
 */
public enum LlmMode {
    STUB,
    REAL
}
