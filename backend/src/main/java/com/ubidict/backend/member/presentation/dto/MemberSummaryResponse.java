package com.ubidict.backend.member.presentation.dto;

import com.ubidict.backend.member.service.model.MemberSummary;

/**
 * 다른 회원을 조회할 때 쓰는 공개용 응답이다. {@code status}·{@code role}은 담지 않는다 —
 * 타인의 사이트 권한·계정 상태까지 공개할 이유가 없다(T-INT-18, {@code docs/plan/CONFLICTS.md} D-62).
 */
public record MemberSummaryResponse(Long memberId, String displayName, String email) {
    public static MemberSummaryResponse from(MemberSummary summary) {
        return new MemberSummaryResponse(summary.memberId(), summary.displayName(), summary.email());
    }
}
