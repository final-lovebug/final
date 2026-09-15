package com.ubidict.backend.member.infra.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 origin 목록. refresh token이 쿠키 기반이라 {@code Access-Control-Allow-Origin: *}는
 * credential 요청에 쓸 수 없어 origin을 명시적으로 나열해야 한다. 콤마로 구분된 문자열 프로퍼티를
 * 그대로 {@code List<String>}으로 바인딩한다(Spring의 relaxed binding).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {}
