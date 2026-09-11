package com.ubidict.backend.document.fixture;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 저장 전 식별자는 null이므로, 식별자가 검증에 필요한 테스트에서만 builder로 주입한다.
 */
public class DocumentFixture {

    /**
     * 본문 길이 경계값. 테스트마다 문자열을 만들지 않는다.
     */
    public static final String MAX_LENGTH_BODY = "가".repeat(DocumentVersion.BODY_MAX_LENGTH);

    public static final String TOO_LONG_BODY = "가".repeat(DocumentVersion.BODY_MAX_LENGTH + 1);

    public static final String DEFAULT_BODY = "회원은 결제할 수 있다.";

    public static DocumentBuilder document() {
        return new DocumentBuilder();
    }

    public static class DocumentBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private String title = "결제 도메인 설계";
        private Long createdBy = 1L;
        private Integer currentVersionNo;

        public DocumentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DocumentBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public DocumentBuilder title(String title) {
            this.title = title;
            return this;
        }

        public DocumentBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
         * 직접 편집 경로를 거치지 않고 특정 현재 버전 상태가 필요한 저장소 테스트에서만 주입한다.
         */
        public DocumentBuilder currentVersionNo(Integer currentVersionNo) {
            this.currentVersionNo = currentVersionNo;
            return this;
        }

        public Document build() {
            Document document = Document.create(workspaceId, title, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(document, "id", id);
            }
            if (currentVersionNo != null) {
                ReflectionTestUtils.setField(document, "currentVersionNo", currentVersionNo);
            }

            return document;
        }
    }
}
