package com.ubidict.backend.revisionlog.infra.port;

import java.util.Optional;

public interface ReviewRequestQueryPort {

    Optional<DocumentRevisionSnapshot> findDocumentRevision(Long reviewRequestId);
}
