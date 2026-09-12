package com.ubidict.backend.reviewrequest.infra.port;

import java.util.List;

/**
 * 승인된 사전 개정안을 새 사전집 버전으로 발행하도록 위임한다(RR-4b).
 *
 * <p>후보어 식별자가 아니라 <b>통합된 용어 목록</b>을 넘긴다(R-12). 식별자를 넘기면 dictionary가 draftdictionary를 조회해야 해
 * 결과물이 원인을 되짚는 역방향 참조가 생긴다.
 *
 * <p>{@code dictionaryId}가 아니라 {@code workspaceId}를 받는다 — 첫 회차에는 가리킬 사전집이 없고, 발행은 「이 워크스페이스의 다음
 * 버전」을 만드는 일이다. {@code baseVersionNo}는 낙관적 검증용이며 현재 활성 버전과 다르면 제공 도메인이 409로 거절한다.
 *
 * @return 새로 발행된 사전집 버전 번호
 */
public interface DictionaryVersionPublishPort {
    int publish(Long workspaceId, int baseVersionNo, List<NewTermSnapshot> terms, Long publishedBy);
}
