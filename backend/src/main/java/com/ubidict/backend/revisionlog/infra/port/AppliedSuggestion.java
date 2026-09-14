package com.ubidict.backend.revisionlog.infra.port;

/** 실제 본문에 반영된 제안어만 개정 이력 항목으로 남긴다. */
public record AppliedSuggestion(String originTerm, String suggestionTerm) {}
