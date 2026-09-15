package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 발행 시점의 활성 사전집 버전을 조회한다(G-7·R-13).
 *
 * <p>두 메서드는 <b>활성 사전집이 없을 때의 의미가 다르다</b>. 문서 발행은 기준 사전집이 없으면 전제가 성립하지 않아 거절해야 하고, 사전 개정안은 첫 회차가 정상
 * 상태라 기준 버전이 0이다.
 */
public interface ActiveDictionaryVersionQueryPort {

    /** 문서 개정본에 기록할 활성 사전집 버전. 활성 사전집이 없으면 DICTIONARY_NOT_FOUND로 거절한다. */
    int activeVersionNo(Long workspaceId);

    /** 사전 개정안의 기준 버전. 사전집이 없는 첫 회차는 0이다. */
    int baseVersionNoForNextVersion(Long workspaceId);
}
