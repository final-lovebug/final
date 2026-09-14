package com.ubidict.backend.common.infra.ai;

/**
 * AI 워커가 처리할 작업의 종류.
 *
 * <p>이름은 <b>외부 계약</b>이다({@code docs/AI_CONTRACT.md}). FastAPI 워커가 이 문자열로 처리 분기를 고르므로, 바꾸면 워커가 함께 깨진다.
 */
public enum LlmJobType {
    TERM_EXTRACTION,
    DOCUMENT_CHECK
}
