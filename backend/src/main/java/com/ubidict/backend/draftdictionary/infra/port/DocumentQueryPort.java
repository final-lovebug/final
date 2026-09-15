package com.ubidict.backend.draftdictionary.infra.port;

/**
 * 문서가 용어 추출 대상인지 본다.
 *
 * <p>G-12의 조건(활성 사전집 버전과 같고 직접 편집되지 않았다)을 그대로 쓴다. 이전 D-15의 {@code isOutdated}는
 * 의미가 뒤집혀 폐기됐다(R-9) — 참이면 제외가 아니라 참이면 포함이다.
 */
public interface DocumentQueryPort {
    boolean isExtractable(Long documentId);
}
