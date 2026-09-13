package com.ubidict.backend.notification.domain;

/**
 * 알림이 무슨 사건을 알리는지 구분한다.
 *
 * <p>MVP1은 리뷰 라이프사이클 5종뿐이다(D-38). {@code docs/DOMAIN.md}가 함께 적은 코멘트등록·대조완료·추출완료는 정의하지 않는다 — 코멘트는 엔티티가 없고,
 * 추출·대조는 작업 테이블 폴링이라(D-34) 구독할 이벤트가 없다. 값만 만들어 두면 아무도 만들지 않는 알림 유형이 enum에 남는다.
 */
public enum NotificationType {
    /** 리뷰 요청이 만들어졌다. 지정된 리뷰어에게 간다. */
    REVIEW_REQUEST_RECEIVED,
    /** 리뷰어가 승인 판정을 냈다. 요청자에게 간다. */
    APPROVED,
    /** 리뷰어가 변경을 요청했다. 요청자에게 간다. */
    CHANGES_REQUESTED,
    /** 승인된 개정안이 반영돼 새 버전이 발행됐다. 워크스페이스 참여자 전원에게 간다. */
    REVISED,
    /** 리뷰 요청이 취소됐다. 요청자에게 간다. */
    CANCELED
}
