package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 사전 초안이 리뷰 요청을 받을 상태인지 초안 도메인에 물어본다(D-44).
 *
 * <p>「교정 완료」만이 아니라 <b>최종 등재 목록이 활성 사전집과 실제로 다른지</b>와 <b>그 정의가 비어 있지 않은지</b>까지 본다(D-21·Y-29). 세 규칙 모두 초안
 * 도메인의 것이므로 어댑터는 제공 도메인에 둔다 — {@link DraftDocumentReviewReadinessPort}와 같은 근거다.
 */
public interface DraftDictionaryReviewReadinessPort {

    void validateReviewReady(Long draftDictionaryId);
}
