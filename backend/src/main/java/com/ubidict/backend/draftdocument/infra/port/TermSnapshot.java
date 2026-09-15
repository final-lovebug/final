package com.ubidict.backend.draftdocument.infra.port;

public record TermSnapshot(Long termId, String preferredForm, String englishName, String definition) {}
