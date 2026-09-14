package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import java.util.Set;

/**
 * 워크스페이스 기준 사전 초안 목록 조회(T-INT-20). {@code DraftDocumentSearchQuery}와 같은
 * 검증 패턴 — DraftDictionary는 엔티티에 {@code workspaceId}를 직접 갖고 있어(DraftDocument와
 * 달리 크로스 도메인 접근 가능 id 목록을 거칠 필요가 없다) {@code workspaceId} 하나로 바로
 * 필터링한다.
 */
public record DraftDictionarySearchQuery(
        Long workspaceId, DraftDictionaryStatus status, int page, int size, String sort, Long memberId) {
    public DraftDictionarySearchQuery {
        if (workspaceId == null || page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        String[] p = sort == null ? new String[] {"createdAt", "desc"} : sort.split(",");
        if (p.length != 2
                || !Set.of("createdAt", "updatedAt", "id").contains(p[0])
                || !(p[1].equalsIgnoreCase("asc") || p[1].equalsIgnoreCase("desc"))) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = p[0] + "," + p[1].toLowerCase();
    }
}
