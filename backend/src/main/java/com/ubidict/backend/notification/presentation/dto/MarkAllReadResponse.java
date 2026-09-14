package com.ubidict.backend.notification.presentation.dto;

public record MarkAllReadResponse(int updated) {

    public static MarkAllReadResponse of(int updated) {
        return new MarkAllReadResponse(updated);
    }
}
