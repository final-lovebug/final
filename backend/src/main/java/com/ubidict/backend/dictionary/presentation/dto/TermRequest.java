package com.ubidict.backend.dictionary.presentation.dto;

import com.ubidict.backend.dictionary.service.model.TermCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TermRequest(
        @NotBlank @Size(max = 100) String preferredForm,
        @Size(max = 100) String englishName,
        @NotBlank String definition) {

    public TermCommand toCommand() {
        return new TermCommand(preferredForm, englishName, definition);
    }
}
