package com.ubidict.backend.document.fixture;

import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.domain.PublishedVersion;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * v2 이상과 「대조를 거친 버전」은 반영(Revise)으로만 생기는데 그 경로가 아직 없다(reviewrequest 도메인). 그래서 리플렉션으로 주입한다.
 */
public class DocumentVersionFixture {

    public static DocumentVersionBuilder documentVersion() {
        return new DocumentVersionBuilder();
    }

    public static class DocumentVersionBuilder {

        private Long id;
        private Long documentId = 1L;
        private Integer versionNo;
        private String body = DocumentFixture.DEFAULT_BODY;
        private Integer dictionaryVersionNo;
        private Long createdBy = 1L;

        public DocumentVersionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DocumentVersionBuilder documentId(Long documentId) {
            this.documentId = documentId;
            return this;
        }

        public DocumentVersionBuilder versionNo(Integer versionNo) {
            this.versionNo = versionNo;
            return this;
        }

        public DocumentVersionBuilder body(String body) {
            this.body = body;
            return this;
        }

        public DocumentVersionBuilder dictionaryVersionNo(Integer dictionaryVersionNo) {
            this.dictionaryVersionNo = dictionaryVersionNo;
            return this;
        }

        public DocumentVersionBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public DocumentVersion build() {
            DocumentVersion version = DocumentVersion.publishFirst(documentId, body, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(version, "id", id);
            }
            if (versionNo != null) {
                ReflectionTestUtils.setField(
                        version,
                        "version",
                        new PublishedVersion(versionNo, OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS)));
            }
            if (dictionaryVersionNo != null) {
                ReflectionTestUtils.setField(version, "dictionaryVersionNo", dictionaryVersionNo);
            }

            return version;
        }
    }
}
