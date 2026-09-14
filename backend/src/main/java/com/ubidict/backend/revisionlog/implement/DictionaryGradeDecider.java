package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import org.springframework.stereotype.Component;

/** 삭제가 하나라도 있으면 기존 문서의 표준어가 사라질 수 있으므로 재검사다(D-59). */
@Component
public class DictionaryGradeDecider {

    public RevisionLogGrade decide(Integer previousVersionNo, DictionaryDiff diff) {
        if (previousVersionNo == null) {
            return RevisionLogGrade.INITIAL;
        }
        if (diff.removedCount() > 0) {
            return RevisionLogGrade.RECHECK_REQUIRED;
        }
        if (diff.addedCount() > 0) {
            return RevisionLogGrade.NEW_TERMS;
        }
        return RevisionLogGrade.NO_IMPACT;
    }
}
