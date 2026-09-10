package com.ubidict.backend.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentVersionTest {

    private static final Long DOCUMENT_ID = 1L;
    private static final Long MEMBER_ID = 7L;

    @DisplayName("업로드본은 v1으로 발행되고 확정일시가 채워진다.")
    @Test
    void publishFirst() {
        // when
        DocumentVersion version = DocumentVersion.publishFirst(DOCUMENT_ID, "회원은 결제할 수 있다.", MEMBER_ID);

        // then
        assertThat(version.versionNo()).isEqualTo(1);
        assertThat(version.publishedAt()).isNotNull();
        assertThat(version.getBody()).isEqualTo("회원은 결제할 수 있다.");
    }

    @DisplayName("업로드본은 대조를 거치지 않았으므로 기준 사전집 버전이 없다.")
    @Test
    void publishFirst_dictionaryVersionIsAbsent() {
        // when
        DocumentVersion version = DocumentVersion.publishFirst(DOCUMENT_ID, "회원은 결제할 수 있다.", MEMBER_ID);

        // then
        assertThat(version.getDictionaryVersionNo()).isNull();
    }

    @DisplayName("본문이 비어 있으면 예외가 발생한다.")
    @Test
    void publishFirst_bodyIsBlank() {
        // when & then
        assertThatThrownBy(() -> DocumentVersion.publishFirst(DOCUMENT_ID, "  \n ", MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_CONTENT);
    }

    @DisplayName("본문이 10,000자를 넘으면 예외가 발생한다.")
    @Test
    void publishFirst_bodyIsTooLong() {
        // given
        String tooLong = "가".repeat(DocumentVersion.BODY_MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> DocumentVersion.publishFirst(DOCUMENT_ID, tooLong, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_CONTENT);
    }

    @DisplayName("본문이 정확히 10,000자면 예외가 발생하지 않는다.")
    @Test
    void publishFirst_bodyIsAtMaxLength() {
        // given
        String maxLength = "가".repeat(DocumentVersion.BODY_MAX_LENGTH);

        // when
        DocumentVersion version = DocumentVersion.publishFirst(DOCUMENT_ID, maxLength, MEMBER_ID);

        // then
        assertThat(version.getBody()).hasSize(DocumentVersion.BODY_MAX_LENGTH);
    }

    /**
     * 이 네 케이스가 「outdated를 저장하지 않고 파생 판정한다」는 결정의 전부다.
     */
    @Nested
    @DisplayName("outdated 판정은")
    class IsOutdated {

        @DisplayName("사전집이 없으면 갱신할 대상이 없으므로 false다.")
        @Test
        void isOutdated_dictionaryIsAbsent() {
            // given
            DocumentVersion version = versionOf(null);

            // when & then
            assertThat(version.isOutdated(null)).isFalse();
        }

        @DisplayName("대조를 거치지 않은 버전이면 true다.")
        @Test
        void isOutdated_versionIsNotContrasted() {
            // given
            DocumentVersion version = versionOf(null);

            // when & then
            assertThat(version.isOutdated(1)).isTrue();
        }

        @DisplayName("기준 사전집 버전이 활성 버전과 다르면 true다.")
        @Test
        void isOutdated_dictionaryVersionIsStale() {
            // given
            DocumentVersion version = versionOf(1);

            // when & then
            assertThat(version.isOutdated(2)).isTrue();
        }

        @DisplayName("기준 사전집 버전이 활성 버전과 같으면 false다.")
        @Test
        void isOutdated_dictionaryVersionIsCurrent() {
            // given
            DocumentVersion version = versionOf(2);

            // when & then
            assertThat(version.isOutdated(2)).isFalse();
        }

        /**
         * 대조를 거친 버전은 반영으로만 생기는데 그 경로가 아직 없어(reviewrequest 도메인) 리플렉션으로 주입한다.
         */
        private DocumentVersion versionOf(Integer dictionaryVersionNo) {
            DocumentVersion version = DocumentVersion.publishFirst(DOCUMENT_ID, "회원은 결제할 수 있다.", MEMBER_ID);
            ReflectionTestUtils.setField(version, "dictionaryVersionNo", dictionaryVersionNo);

            return version;
        }
    }
}
