package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;

public record DictionaryTermChange(RevisionLogChangeType changeType, TermSnapshot term, String detail) {}
