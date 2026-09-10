package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.LabelResult;

public record LabelResponse(Long labelId, String name) {

    public static LabelResponse from(LabelResult result) {
        return new LabelResponse(result.labelId(), result.name());
    }
}
