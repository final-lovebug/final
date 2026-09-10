package com.ubidict.backend.document.service.model;

import com.ubidict.backend.document.domain.Label;

public record LabelResult(Long labelId, String name) {

    public static LabelResult from(Label label) {
        return new LabelResult(label.getId(), label.getName());
    }
}
