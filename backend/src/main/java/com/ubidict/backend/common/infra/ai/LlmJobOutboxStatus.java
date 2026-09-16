package com.ubidict.backend.common.infra.ai;

public enum LlmJobOutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    CANCELLED
}
