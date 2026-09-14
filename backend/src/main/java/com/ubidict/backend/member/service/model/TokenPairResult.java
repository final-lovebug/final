package com.ubidict.backend.member.service.model;

import com.ubidict.backend.member.domain.MemberRole;

public record TokenPairResult(String accessToken, String refreshToken, MemberRole role) {}
