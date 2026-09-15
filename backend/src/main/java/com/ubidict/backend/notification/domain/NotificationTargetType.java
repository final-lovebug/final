package com.ubidict.backend.notification.domain;

/**
 * 알림을 눌렀을 때 이동할 대상의 종류. {@code targetId}와 한 쌍으로 쓴다.
 *
 * <p>이동 경로 문자열을 저장하지 않는 이유 — 경로는 화면의 라우팅 규칙이고, 저장하면 라우팅을 바꿀 때 과거 알림 행까지 고쳐야 한다(D-40).
 */
public enum NotificationTargetType {
    DOCUMENT,
    DICTIONARY,
    REVIEW_REQUEST
}
