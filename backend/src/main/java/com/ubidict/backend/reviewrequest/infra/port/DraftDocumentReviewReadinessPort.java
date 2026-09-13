package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 문서 초안이 리뷰 요청을 받을 상태인지 초안 도메인에 물어본다(D-44).
 *
 * <p>판정 규칙과 그 ErrorCode는 초안 도메인의 도메인 로직이므로 이 포트의 어댑터는 <b>제공 도메인</b>에 둔다. 발행 위임 포트와 같은 갈림이다(D-33) — 소비 도메인에
 * 두면 어댑터가 리포지토리만 보고 규칙을 다시 구현하게 된다.
 *
 * <p>조건을 만족하지 못하면 예외를 던진다. boolean으로 감싸면 거절 이유를 소비 도메인이 다시 만들어야 한다(D-19가 접근 검증에서 세운 근거와 같다).
 */
public interface DraftDocumentReviewReadinessPort {

    void validateReviewReady(Long draftDocumentId);
}
