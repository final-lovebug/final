package com.ubidict.backend.member.presentation;

import com.ubidict.backend.member.presentation.dto.DevLoginRequest;
import com.ubidict.backend.member.presentation.dto.TokenResponse;
import com.ubidict.backend.member.service.DevLoginService;
import com.ubidict.backend.member.service.model.TokenPairResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 개발 전용 — Google 로그인 없이 이미 존재하는 회원으로 바로 로그인한다. 다른 팀원이 자기
 * 도메인 API를 테스트할 때 Google Test User 등록·OAuth 왕복 없이 토큰을 받을 수 있게 하기 위함이다.
 *
 * <p>{@code /api/auth/**} 하위라 {@code SecurityConfig}의 기존 permitAll 규칙을 그대로 타고,
 * {@code local} 프로필에서만 빈이 등록돼 배포 환경엔 이 경로 자체가 존재하지 않는다.
 */
@Profile("local")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/dev")
public class DevAuthController {

    private final DevLoginService devLoginService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody DevLoginRequest request) {
        TokenPairResult result = devLoginService.loginAs(request.memberId());

        ResponseCookie cookie = refreshTokenCookieProvider.issue(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(TokenResponse.from(result));
    }
}
