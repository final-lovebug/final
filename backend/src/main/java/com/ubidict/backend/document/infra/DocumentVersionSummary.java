package com.ubidict.backend.document.infra;

import com.ubidict.backend.document.domain.DocumentVersion;
import java.time.OffsetDateTime;

/**
 * 본문을 뺀 버전 조회 결과.
 *
 * <p>목록과 버전 이력은 본문을 보여 주지 않는다. 엔티티를 그대로 읽으면 10,000자짜리 본문을 문서 수만큼 끌어오게 되므로 필요한 컬럼만 뽑는다.
 */
public record DocumentVersionSummary(
        Long documentId, int versionNo, Integer dictionaryVersionNo, OffsetDateTime publishedAt, Long publishedBy) {

    /**
     * 판정 규칙은 도메인이 갖는다. 목록 조회가 본문을 빼고 읽는다는 사정 때문에 규칙이 두 벌이 되면 안 된다.
     */
    public boolean isOutdated(Integer activeDictionaryVersionNo) {
        return DocumentVersion.isOutdated(dictionaryVersionNo, activeDictionaryVersionNo);
    }
}
