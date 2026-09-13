package com.ubidict.backend.reviewrequest.infra.port;

/** 문서 개정본에 기록할 발행 시점의 활성 사전집 버전을 조회한다(G-7·R-13). */
public interface ActiveDictionaryVersionQueryPort {

    int activeVersionNo(Long workspaceId);
}
