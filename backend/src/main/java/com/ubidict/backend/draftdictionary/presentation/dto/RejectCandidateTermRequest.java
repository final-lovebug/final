package com.ubidict.backend.draftdictionary.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectCandidateTermRequest(@NotBlank String rejectReason) {}
