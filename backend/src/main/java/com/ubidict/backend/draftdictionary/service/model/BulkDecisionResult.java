package com.ubidict.backend.draftdictionary.service.model;

import java.util.List;

public record BulkDecisionResult(List<Long> succeeded, List<Failure> failed) {
    public record Failure(Long candidateTermId, String code, String message) {}
}
