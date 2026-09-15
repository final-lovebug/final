package com.ubidict.backend.member.infra.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * JWT 기반 stateless 인증/인가를 구성한다.
 *
 * <p>{@code /api/auth/**}는 access token이 없거나 만료된 상태에서도 호출돼야 하므로 인증을
 * 요구하지 않는다({@code docs/API.md}). 그 외 API는 {@code /api/members/**}를 포함해 모두
 * 인증을 요구한다(9/9 확정). Swagger UI는 로컬 개발 편의를 위해 열어둔 실무 판단이며, 도메인 정책
 * 문서에 근거를 둔 결정은 아니다.
 *
 * <p><b>actuator는 경로를 나눠 다룬다</b>(D-97). {@code /actuator/health}·
 * {@code /actuator/health/**}·{@code /actuator/info}는 항상 열어 둔다 —
 * {@code deploy/scripts/validate.sh}가 readiness를 폴링하므로 이것을 닫으면 배포가 실패한다.
 * 나머지({@code /actuator/prometheus} 등)는 {@link ActuatorSecurityProperties}가 켜 줄 때만 열리고
 * 기본은 {@code ADMIN} 권한을 요구한다.
 *
 * <p>인가 판단을 {@code authentication.isAuthenticated()}로 직접 쓰지 않는다 — <b>익명 인증 토큰도
 * 그 값이 {@code true}라</b> 무인증 요청이 통과한다. DSL의 {@code hasRole}·{@code authenticated}는
 * trust resolver로 익명을 가려 주므로 그쪽을 쓴다.
 *
 * <p>{@code /api/internal/**}은 AI 워커(FastAPI)가 작업 결과를 돌려주는 서버-투-서버 경로다. 회원
 * principal이 없으므로 인증을 요구할 수 없고, 대신 <b>작업마다 발행되는 1회용 상관 식별자를 본문에서 받아
 * 작업 행의 값과 대조</b>한다(D-70). 이것만으로는 {@code NFR-AI-002}가 충족되지 않는다 — 배포 시
 * 보안 그룹·인그레스로 워커 출발지만 이 경로에 닿게 제한해야 한다.
 *
 * <p>{@code /oauth2/**}(로그인 시작)와 {@code /login/oauth2/**}(Google 콜백)도 인증 전
 * 단계라 permitAll이다. 로그인 성공/실패는 세션이 아니라
 * {@link GoogleOAuth2LoginSuccessHandler}/{@link GoogleOAuth2LoginFailureHandler}가 프론트엔드
 * 리다이렉트로 이어받는다 — stateless 정책과 맞추기 위해 OAuth2 로그인 자체의 세션 인가
 * 상태는 유지하지 않는다.
 *
 * <p>서블릿 웹 애플리케이션일 때만 등록한다 — {@link JwtAuthenticationEntryPoint}/
 * {@link JwtAccessDeniedHandler}가 필요로 하는 {@code HandlerExceptionResolver}가
 * {@code webEnvironment = WebEnvironment.NONE}(예: {@code IntegrationTestSupport} 기반 서비스
 * 통합 테스트)에서는 존재하지 않아, 그런 컨텍스트까지 이 설정을 로드하려다 실패하지 않게 한다.
 *
 * <p>refresh token을 쿠키로 주고받으므로(자격증명 포함 요청) CORS는 {@code allowCredentials(true)}와
 * 함께 명시적 origin 목록({@link CorsProperties})만 허용한다 — {@code Access-Control-Allow-Origin: *}는
 * 자격증명 포함 요청에 쓸 수 없다.
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final GoogleOAuth2LoginSuccessHandler googleOAuth2LoginSuccessHandler;
    private final GoogleOAuth2LoginFailureHandler googleOAuth2LoginFailureHandler;
    private final CorsProperties corsProperties;
    private final ActuatorSecurityProperties actuatorSecurityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/auth/**").permitAll();
                    authorize.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                    authorize
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml")
                            .permitAll();
                    authorize
                            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                            .permitAll();
                    if (actuatorSecurityProperties.permitAll()) {
                        authorize.requestMatchers("/actuator/**").permitAll();
                    } else {
                        authorize.requestMatchers("/actuator/**").hasRole("ADMIN");
                    }
                    authorize.requestMatchers("/api/internal/**").permitAll();
                    authorize.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .oauth2Login(oauth2 -> oauth2.successHandler(googleOAuth2LoginSuccessHandler)
                        .failureHandler(googleOAuth2LoginFailureHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
