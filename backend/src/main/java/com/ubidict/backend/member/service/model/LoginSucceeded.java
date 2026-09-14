package com.ubidict.backend.member.service.model;

/** 이미 가입된 회원의 로그인 성공 — access/refresh 토큰이 발급됐다. */
public record LoginSucceeded(TokenPairResult tokenPair) implements OAuthExchangeOutcome {}
