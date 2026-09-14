package com.ubidict.backend.member.service.model;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.domain.MemberStatus;

public record MemberResult(Long memberId, String email, String displayName, MemberStatus status, MemberRole role) {

    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(), member.getEmail(), member.getDisplayName(), member.getStatus(), member.getRole());
    }
}
