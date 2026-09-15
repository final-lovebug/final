package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import java.util.Set;
import org.springframework.data.domain.Sort;

public record ReviewRequestSearchQuery(
        Long workspaceId,
        ReviewRequestType type,
        ReviewRequestStatus status,
        Long requesterId,
        Long reviewerMemberId,
        int page,
        int size,
        String sort) {
    public ReviewRequestSearchQuery {
        if (workspaceId == null) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_WORKSPACE_ID_REQUIRED);
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        sort = sort == null ? "createdAt,desc" : sort;
        String[] parts = sort.split(",", -1);
        if (parts.length != 2
                || !Set.of("createdAt", "updatedAt", "id").contains(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
    }

    public String sortField() {
        return sort.split(",", -1)[0];
    }

    public Sort.Direction sortDirection() {
        return Sort.Direction.fromString(sort.split(",", -1)[1]);
    }
}
