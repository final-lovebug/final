package com.ubidict.backend.revisionlog.infra.port;

public record TermSnapshot(String preferredForm, String englishName, String definition) {}
