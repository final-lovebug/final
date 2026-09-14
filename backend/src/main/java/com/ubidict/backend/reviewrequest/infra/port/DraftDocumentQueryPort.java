package com.ubidict.backend.reviewrequest.infra.port;

import java.util.Optional;

/**
 * 발행할 문서 개정안의 근거가 되는 초안을 읽는다(RR-4b).
 */
public interface DraftDocumentQueryPort {
    Optional<DraftDocumentSnapshot> read(Long draftDocumentId);
}
