package com.ubidict.backend.member.service.model;

/**
 * 교환 코드 로그인 결과. 이미 가입된 회원이면 {@link LoginSucceeded}, 최초 로그인(신규
 * 식별자)이면 {@link RegistrationRequired}다({@code docs/API.md} "콜백 및 토큰 교환").
 */
public sealed interface OAuthExchangeOutcome permits LoginSucceeded, RegistrationRequired {}
