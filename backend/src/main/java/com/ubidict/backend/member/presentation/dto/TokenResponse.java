package com.ubidict.backend.member.presentation.dto;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.service.model.TokenPairResult;

public record TokenResponse(String accessToken, MemberRole role) {

    public static TokenResponse from(TokenPairResult result) {
        return new TokenResponse(result.accessToken(), result.role());
    }
}
