package com.ubidict.backend.dictionary.service.model;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.Term;
import java.time.OffsetDateTime;
import java.util.List;

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
        List<TermResult> terms) {

    public static DictionaryResult of(Dictionary dictionary, List<Term> terms) {
        return new DictionaryResult(
                dictionary.getId(),
                dictionary.getWorkspaceId(),
                dictionary.versionNo(),
                dictionary.getStatus(),
                dictionary.getVersion().publishedAt(),
                dictionary.getCreatedBy(),
                terms.stream().map(TermResult::from).toList());
    }
}
