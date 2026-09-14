package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.service.model.BulkDecisionResult;
import java.util.List;

public record BulkDecisionResponse(List<Long> succeeded, List<Failure> failed) {
    public record Failure(Long candidateTermId, String code, String message) {}

    public static BulkDecisionResponse from(BulkDecisionResult r) {
        return new BulkDecisionResponse(
                r.succeeded(),
                r.failed().stream()
                        .map(f -> new Failure(f.candidateTermId(), f.code(), f.message()))
                        .toList());
    }
}
