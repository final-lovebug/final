package com.ubidict.backend.member.service.model;

/**
 * 최초 로그인(신규 식별자) — 아직 {@code Member} row가 없다. 닉네임을 입력받아
 * {@code POST /api/auth/oauth/google/complete-registration}을 호출해야 로그인이 끝난다.
 */
public record RegistrationRequired(String registrationToken) implements OAuthExchangeOutcome {}
