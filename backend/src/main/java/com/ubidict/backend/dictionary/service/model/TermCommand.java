package com.ubidict.backend.dictionary.service.model;

import com.ubidict.backend.dictionary.domain.NewTerm;

public record TermCommand(String preferredForm, String englishName, String definition) {

    public NewTerm toNewTerm() {
        return new NewTerm(preferredForm, englishName, definition);
    }
}
