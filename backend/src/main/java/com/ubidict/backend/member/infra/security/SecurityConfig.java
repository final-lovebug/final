package com.ubidict.backend.member.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 stateless 인증/인가를 구성한다.
 *
 * <p>{@code /api/auth/**}는 access token이 없거나 만료된 상태에서도 호출돼야 하므로 인증을
 * 요구하지 않는다({@code docs/API.md}). 그 외 API는 {@code /api/members/**}를 포함해 모두
 * 인증을 요구한다(9/9 확정). Swagger UI·actuator는 로컬 개발 편의를 위해 열어둔 실무 판단이며,
 * 도메인 정책 문서에 근거를 둔 결정은 아니다 — 배포 환경 노출 여부는 별도로 검토한다.
 *
 * <p>{@code /oauth2/**}(로그인 시작)와 {@code /login/oauth2/**}(Google 콜백)도 인증 전
 * 단계라 permitAll이다. 로그인 성공/실패는 세션이 아니라
 * {@link GoogleOAuth2LoginSuccessHandler}/{@link GoogleOAuth2LoginFailureHandler}가 프론트엔드
 * 리다이렉트로 이어받는다 — stateless 정책과 맞추기 위해 OAuth2 로그인 자체의 세션 인가
 * 상태는 유지하지 않는다.
 */
@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final GoogleOAuth2LoginSuccessHandler googleOAuth2LoginSuccessHandler;
    private final GoogleOAuth2LoginFailureHandler googleOAuth2LoginFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .oauth2Login(oauth2 -> oauth2.successHandler(googleOAuth2LoginSuccessHandler)
                        .failureHandler(googleOAuth2LoginFailureHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
