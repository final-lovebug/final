package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization} 헤더의 access token을 검증해 {@link SecurityContextHolder}에 인증
 * 정보를 채운다.
 *
 * <p>헤더가 아예 없으면 그냥 통과시킨다 — {@code /api/auth/**} 같은 permitAll 경로는 인증 없이도
 * 통과해야 하고, 보호된 경로라면 이후 인가 단계에서 {@link JwtAuthenticationEntryPoint}가
 * {@link AuthErrorCode#AUTH_TOKEN_MISSING}으로 응답한다. 반대로 헤더는 있는데 문제가 있으면
 * (형식 오류·서명 불일치·만료·role 누락) 원인이 되는 예외를 요청 attribute에 남겨, entry point가
 * 그 원인 그대로 응답 코드를 결정하게 한다.
 */
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_EXCEPTION_ATTRIBUTE = "com.ubidict.backend.member.auth.exception";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && !header.isBlank()) {
            try {
                authenticate(header);
            } catch (BusinessException e) {
                request.setAttribute(AUTH_EXCEPTION_ATTRIBUTE, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String header) {
        if (!header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID, "Authorization header is not Bearer scheme.");
        }

        String token = header.substring(BEARER_PREFIX.length());
        Claims claims = jwtProvider.parseAccessToken(token);
        MemberRole role = resolveRole(claims);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication =
                new UsernamePasswordAuthenticationToken(Long.valueOf(claims.getSubject()), token, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * role 클레임은 권한 도입(WLSH-75) 이전에 발급된 토큰에는 없다. 그런 토큰은 무효로
     * 처리한다({@code docs/API.md} 인증 실패 응답 표).
     */
    private MemberRole resolveRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID, "Access token has no role claim.");
        }
        try {
            return MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID, "Access token has unknown role: " + role, e);
        }
    }
}
