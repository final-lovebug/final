package com.ubidict.backend.dictionary.service.model;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import java.time.OffsetDateTime;

/**
 * 도메인 모델이 JPA 엔티티를 겸하므로 result에 담지 않고 필요한 값만 옮긴다(API.md 공통 규칙).
 */
public record DictionaryResult(
        Long dictionaryId,
        Long workspaceId,
        int versionNo,
        DictionaryStatus status,
        OffsetDateTime publishedAt,
        Long createdBy,
        PageResult<TermResult> terms) {

    public static DictionaryResult of(Dictionary dictionary, PageResult<TermResult> terms) {
        return new DictionaryResult(
                dictionary.getId(),
                dictionary.getWorkspaceId(),
                dictionary.versionNo(),
                dictionary.getStatus(),
                dictionary.getVersion().publishedAt(),
                dictionary.getCreatedBy(),
                terms);
    }
}
