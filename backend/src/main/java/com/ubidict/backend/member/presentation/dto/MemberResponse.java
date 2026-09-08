package com.ubidict.backend.member.presentation.dto;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.service.model.MemberResult;

public record MemberResponse(Long memberId, String email, String displayName, MemberStatus status, MemberRole role) {

    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(
                result.memberId(), result.email(), result.displayName(), result.status(), result.role());
    }
}
