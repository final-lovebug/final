package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.ReexamineResult;
import java.time.OffsetDateTime;

public record ReexamineResponse(int round, OffsetDateTime performedAt) {

    public static ReexamineResponse from(ReexamineResult result) {
        return new ReexamineResponse(result.round(), result.performedAt());
    }
}
