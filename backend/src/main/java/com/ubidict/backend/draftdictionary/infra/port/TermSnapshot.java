package com.ubidict.backend.draftdictionary.infra.port;

public record TermSnapshot(Long termId, String preferredForm, String englishName, String definition) {}
