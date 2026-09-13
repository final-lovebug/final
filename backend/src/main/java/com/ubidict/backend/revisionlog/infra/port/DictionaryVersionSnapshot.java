package com.ubidict.backend.revisionlog.infra.port;

import java.time.OffsetDateTime;

/** 발행 이벤트에 없는 사전집 버전의 작성자와 확정 시각을 다시 읽기 위한 스냅샷. */
public record DictionaryVersionSnapshot(Long dictionaryId, Long publishedBy, OffsetDateTime publishedAt) {}
