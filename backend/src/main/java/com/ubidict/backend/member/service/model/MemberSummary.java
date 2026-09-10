package com.ubidict.backend.member.service.model;

import com.ubidict.backend.member.domain.MemberStatus;

/**
 * 다른 도메인이 Member를 참조할 때 쓰는 조회 전용 모델. {@code Member} 도메인
 * 모델을 직접 노출하지 않기 위한 경계다.
 */
public record MemberSummary(Long memberId, String displayName, String email, MemberStatus status) {}
