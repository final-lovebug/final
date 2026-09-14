package com.ubidict.backend.revisionlog.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** 개정 이력 타임라인의 축·대상·페이지 조건. */
public record RevisionLogSearchQuery(
        Long workspaceId,
        Long memberId,
        RevisionLogTargetType targetType,
        Long targetId,
        int page,
        int size,
        String sort) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("publishedAt");

    public RevisionLogSearchQuery {
        if (workspaceId == null || memberId == null || targetType == null || page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = normalizeSort(sort);
    }

    public static RevisionLogSearchQuery of(
            Long workspaceId,
            Long memberId,
            RevisionLogTargetType targetType,
            Long targetId,
            Integer page,
            Integer size,
            String sort) {
        return new RevisionLogSearchQuery(
                workspaceId,
                memberId,
                targetType,
                targetId,
                page == null ? 0 : page,
                size == null ? DEFAULT_SIZE : size,
                sort);
    }

    public Pageable toPageable() {
        String[] parts = sort.split(",", -1);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(parts[1]), parts[0]));
    }

    private static String normalizeSort(String sort) {
        String candidate = sort == null ? "publishedAt,desc" : sort;
        String[] parts = candidate.split(",", -1);
        if (parts.length != 2
                || !SORTABLE_FIELDS.contains(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        return parts[0] + "," + parts[1].toLowerCase();
    }
}
