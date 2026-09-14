package com.ubidict.backend.revisionlog.implement;

import java.util.List;

public record DictionaryDiff(List<DictionaryTermChange> changes, int addedCount, int changedCount, int removedCount) {}
