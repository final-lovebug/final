package com.ubidict.backend.revisionlog.fixture;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import java.time.OffsetDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public class RevisionLogFixture {

    public static DictionaryRevisionLogBuilder dictionaryLog() {
        return new DictionaryRevisionLogBuilder();
    }

    public static DocumentRevisionLogBuilder documentLog() {
        return new DocumentRevisionLogBuilder();
    }

    public static class DictionaryRevisionLogBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private Long dictionaryId = 10L;
        private int versionNo = 2;
        private Integer previousVersionNo = 1;
        private RevisionLogGrade grade = RevisionLogGrade.NEW_TERMS;
        private int addedCount = 2;
        private int changedCount = 1;
        private int removedCount;
        private int affectedDocumentCount;
        private String summary = "용어 2개 추가";
        private Long publishedBy = 3L;
        private OffsetDateTime publishedAt = OffsetDateTime.parse("2026-09-13T12:00:00Z");

        public DictionaryRevisionLogBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public DictionaryRevisionLogBuilder dictionaryId(Long dictionaryId) {
            this.dictionaryId = dictionaryId;
            return this;
        }

        public DictionaryRevisionLogBuilder versionNo(int versionNo) {
            this.versionNo = versionNo;
            return this;
        }

        public DictionaryRevisionLogBuilder previousVersionNo(Integer previousVersionNo) {
            this.previousVersionNo = previousVersionNo;
            return this;
        }

        public RevisionLog build() {
            RevisionLog revisionLog = RevisionLog.forDictionary(
                    workspaceId,
                    dictionaryId,
                    versionNo,
                    previousVersionNo,
                    grade,
                    addedCount,
                    changedCount,
                    removedCount,
                    affectedDocumentCount,
                    summary,
                    publishedBy,
                    publishedAt);
            if (id != null) {
                ReflectionTestUtils.setField(revisionLog, "id", id);
            }
            return revisionLog;
        }
    }

    public static class DocumentRevisionLogBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private Long documentId = 20L;
        private int versionNo = 2;
        private Integer previousVersionNo = 1;
        private RevisionOrigin origin = RevisionOrigin.DIRECT_EDIT;
        private Integer baseDictionaryVersionNo = 5;
        private int changedCount;
        private String summary = "직접 편집";
        private Long publishedBy = 3L;
        private OffsetDateTime publishedAt = OffsetDateTime.parse("2026-09-13T12:00:00Z");

        public DocumentRevisionLogBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public DocumentRevisionLogBuilder documentId(Long documentId) {
            this.documentId = documentId;
            return this;
        }

        public RevisionLog build() {
            RevisionLog revisionLog = RevisionLog.forDocument(
                    workspaceId,
                    documentId,
                    versionNo,
                    previousVersionNo,
                    origin,
                    baseDictionaryVersionNo,
                    changedCount,
                    summary,
                    publishedBy,
                    publishedAt);
            if (id != null) {
                ReflectionTestUtils.setField(revisionLog, "id", id);
            }
            return revisionLog;
        }
    }
}
