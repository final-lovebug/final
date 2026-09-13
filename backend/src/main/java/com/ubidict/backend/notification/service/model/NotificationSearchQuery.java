package com.ubidict.backend.notification.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 알림 목록 조회 조건. 페이징 규격은 {@code docs/API.md} «페이징·정렬 규격»을 따른다.
 *
 * <p><b>정렬 화이트리스트는 {@code createdAt} 하나다.</b> 알림은 최신순으로만 보므로 다른 축을 열면 인덱스 없는 컬럼으로 전체 스캔이 난다.
 *
 * <p>{@code recipientId}를 요청에서 받지 않는 이유 — 목록은 <b>항상 요청자 본인의 것</b>이다. 파라미터로 받으면 남의 알림을 조회하는 경로가 생긴다.
 */
public record NotificationSearchQuery(
        Long workspaceId, Long memberId, boolean unreadOnly, int page, int size, String sort) {

    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 20;
    private static final String SORTABLE_FIELD = "createdAt";

    public NotificationSearchQuery {
        if (page < 0) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        // 정렬은 여기서 미리 검증한다. toPageable()까지 미루면 service를 타기 전에는 잘못된 정렬이 걸러지지 않는다.
        parseSort(sort);
    }

    public static NotificationSearchQuery of(
            Long workspaceId, Long memberId, Boolean unreadOnly, Integer page, Integer size, String sort) {
        return new NotificationSearchQuery(
                workspaceId,
                memberId,
                Boolean.TRUE.equals(unreadOnly),
                page == null ? 0 : page,
                size == null ? DEFAULT_SIZE : size,
                sort);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, parseSort(sort));
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, SORTABLE_FIELD);
        }

        String[] parts = sort.split(",");
        String field = parts[0].strip();
        if (!SORTABLE_FIELD.equals(field)) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1) {
            direction = Sort.Direction.fromOptionalString(parts[1].strip())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST));
        }

        return Sort.by(direction, field);
    }
}
