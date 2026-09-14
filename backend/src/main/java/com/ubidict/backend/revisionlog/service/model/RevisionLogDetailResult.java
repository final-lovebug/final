package com.ubidict.backend.revisionlog.service.model;

import java.util.List;

public record RevisionLogDetailResult(RevisionLogResult revisionLog, List<RevisionLogEntryResult> entries) {}
