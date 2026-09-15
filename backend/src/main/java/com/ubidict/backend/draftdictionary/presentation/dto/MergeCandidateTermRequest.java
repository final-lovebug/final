package com.ubidict.backend.draftdictionary.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record MergeCandidateTermRequest(@NotNull Long mergeTargetTermId) {}
