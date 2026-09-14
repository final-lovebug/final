package com.ubidict.backend.member.presentation.dto;

import com.ubidict.backend.member.domain.OAuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 테스트/관리자용 회원 생성 요청. 실제 소셜 로그인을 통한 가입은 로그인
 * 도메인(별도 티켓)이 담당한다.
 */
public record CreateMemberRequest(
        @Email @NotBlank String email,
        @NotBlank String displayName,
        @NotNull OAuthProvider provider,
        @NotBlank String providerId) {}
