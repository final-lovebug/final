package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.ReviseResult;
import java.time.OffsetDateTime;

public record ReviseResponse(int resultVersionNo, OffsetDateTime performedAt) {

    public static ReviseResponse from(ReviseResult result) {
        return new ReviseResponse(result.resultVersionNo(), result.performedAt());
    }
}
