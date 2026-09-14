package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import org.springframework.stereotype.Component;

@Component
public class RevisionSummaryFactory {

    public String forDictionary(RevisionLogGrade grade, DictionaryDiff diff) {
        if (grade == RevisionLogGrade.INITIAL) {
            return "사전집 최초 발행";
        }
        if (grade == RevisionLogGrade.NO_IMPACT) {
            return "용어 " + diff.changedCount() + "개 변경";
        }
        if (grade == RevisionLogGrade.NEW_TERMS) {
            return "용어 " + diff.addedCount() + "개 추가";
        }
        return "용어 " + diff.removedCount() + "개 삭제";
    }

    public String forDocument(RevisionOrigin origin, int changedCount) {
        if (origin == RevisionOrigin.UPLOAD) {
            return "최초 업로드";
        }
        if (origin == RevisionOrigin.DIRECT_EDIT) {
            return "직접 편집";
        }
        return "제안어 " + changedCount + "개 적용";
    }
}
