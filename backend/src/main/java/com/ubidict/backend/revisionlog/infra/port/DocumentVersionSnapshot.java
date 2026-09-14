package com.ubidict.backend.revisionlog.infra.port;

import java.time.OffsetDateTime;

/** 문서 버전 이력을 엔티티 없이 개정 이력 도메인에 전달하는 스냅샷. */
public record DocumentVersionSnapshot(
        int versionNo, Integer dictionaryVersionNo, boolean edited, OffsetDateTime publishedAt, Long publishedBy) {}
