package com.ubidict.backend.draftdocument.fixture;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import org.springframework.test.util.ReflectionTestUtils;

public class DraftDocumentFixture {

    public static DraftDocumentBuilder draftDocument() {
        return new DraftDocumentBuilder();
    }

    public static class DraftDocumentBuilder {

        private Long id;
        private Long documentId = 1L;
        private int baseVersionNo = 1;
        private String draftBody = "회원은 결제할 수 있다.";
        private Long requestedBy = 1L;
        private Long createdBy = 1L;
        private DraftDocumentStatus status;

        public DraftDocumentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DraftDocumentBuilder documentId(Long documentId) {
            this.documentId = documentId;
            return this;
        }

        public DraftDocumentBuilder baseVersionNo(int baseVersionNo) {
            this.baseVersionNo = baseVersionNo;
            return this;
        }

        public DraftDocumentBuilder draftBody(String draftBody) {
            this.draftBody = draftBody;
            return this;
        }

        public DraftDocumentBuilder requestedBy(Long requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public DraftDocumentBuilder status(DraftDocumentStatus status) {
            this.status = status;
            return this;
        }

        public DraftDocument build() {
            DraftDocument draftDocument =
                    DraftDocument.create(documentId, baseVersionNo, draftBody, requestedBy, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(draftDocument, "id", id);
            }
            if (status != null) {
                ReflectionTestUtils.setField(draftDocument, "status", status);
            }

            return draftDocument;
        }
    }
}
