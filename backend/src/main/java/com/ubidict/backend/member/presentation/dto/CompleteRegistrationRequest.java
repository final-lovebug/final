package com.ubidict.backend.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteRegistrationRequest(
        @NotBlank String registrationToken, @NotBlank String displayName) {}
