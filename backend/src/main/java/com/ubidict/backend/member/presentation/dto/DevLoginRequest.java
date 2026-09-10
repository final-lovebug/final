package com.ubidict.backend.member.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record DevLoginRequest(@NotNull Long memberId) {}
