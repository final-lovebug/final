package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 승인된 문서 개정안을 새 문서 버전으로 발행하도록 위임한다(RR-4b).
 *
 * <p>{@code dictionaryVersionNo}를 함께 넘긴다 — 반영본의 기준 사전집 버전은 발행 시점의 최신 사전집 버전을 따른다(G-7·R-13).
 *
 * <p>조회 포트와 달리 <b>어댑터는 제공 도메인에 있다</b>(D-33). 발행은 document 도메인의 로직이라 어댑터가 그쪽 service를 써야 한다.
 *
 * @return 새로 발행된 버전 번호
 */
public interface DocumentVersionPublishPort {
    int publish(Long documentId, int baseVersionNo, String body, int dictionaryVersionNo, Long publishedBy);
}
