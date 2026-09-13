package com.ubidict.backend.notification.infra.port;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;

/**
 * 알림을 만드는 데 필요한 리뷰 요청 정보만 옮긴 것.
 *
 * <p>엔티티를 포트 시그니처에 넣지 않는다 — {@code docs/ARCHITECTURE.md} «크로스 도메인 조회»의 규칙이다. 지연 로딩 프록시가 소비 도메인으로 새는 경로를
 * 막는다.
 */
public record ReviewRequestSnapshot(Long workspaceId, ReviewRequestType type, String title, Long requesterId) {}
