package com.ubidict.backend.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TextRange(
        @Column(name = "start_offset", nullable = false) int startOffset,
        @Column(name = "end_offset", nullable = false) int endOffset) {
    public TextRange {
        if (startOffset < 0 || endOffset < 0 || startOffset > endOffset) {
            throw new IllegalArgumentException("Invalid text range");
        }
    }
}
