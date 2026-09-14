package com.ubidict.backend.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRequest(@NotBlank String displayName) {}
