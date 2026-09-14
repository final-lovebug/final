package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.infra.port.DocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentImpactCounter {

    private final DocumentQueryPort documentQueryPort;

    /** 영향도 스냅샷은 삭제로 표준어가 사라질 수 있는 재검사 등급에서만 센다. */
    public int count(Long workspaceId, int dictionaryVersionNo, RevisionLogGrade grade) {
        if (grade != RevisionLogGrade.RECHECK_REQUIRED) {
            return 0;
        }
        return Math.toIntExact(documentQueryPort.countAlignedBelow(workspaceId, dictionaryVersionNo));
    }
}
