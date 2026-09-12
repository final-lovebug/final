package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 새 사전집 버전에 실릴 용어 하나.
 *
 * <p>식별자를 담지 않는다 — 발행은 이전 버전에서 복사하지 않고 넘어온 목록만으로 버전을 구성한다(R-12).
 */
public record NewTermSnapshot(String preferredForm, String englishName, String definition) {}
