package com.ubidict.backend.document.fixture;

import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.domain.PublishedVersion;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 직접 편집·교정 반영 경로를 거치지 않고 특정 버전 상태가 필요한 저장소 테스트에서 리플렉션으로 값을 주입한다.
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
        private boolean edited;
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

        public DocumentVersionBuilder edited(boolean edited) {
            this.edited = edited;
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
            ReflectionTestUtils.setField(version, "edited", edited);

            return version;
        }
    }
}
