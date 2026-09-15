package com.ubidict.backend.member.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * actuator 경로를 인증 없이 열지 여부. 기본값은 {@code false}(닫힘)이고 {@code local}·{@code dev}만
 * 켠다(D-97).
 *
 * <p>{@code management.endpoints.web.exposure.include}에 {@code prometheus}를 더하는 것만으로는
 * 「눈으로 확인하는 수단」이 되지 못한다 — 노출은 됐는데 필터 체인이 401을 내기 때문이다. 노출과
 * 인가는 별개의 스위치이며 이 값이 후자다.
 *
 * <p>{@code health}·{@code info}는 이 값과 무관하게 항상 열려 있다. 배포 검증 스크립트가
 * {@code /actuator/health/readiness}를 폴링하기 때문이다.
 */
@ConfigurationProperties(prefix = "app.security.actuator")
public record ActuatorSecurityProperties(boolean permitAll) {}
