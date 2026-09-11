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
     * 정렬 상태를 저장하지 않고 현재 활성 사전집과 버전 상태로 파생한다.
     */
    @Nested
    @DisplayName("aligned 판정은")
    class IsAligned {

        @DisplayName("활성 사전집이 없으면 정렬된 것으로 본다.")
        @Test
        void isAligned_dictionaryIsAbsent() {
            // given
            DocumentVersion version = versionOf(null);

            // when & then
            assertThat(version.isAligned(null)).isTrue();
        }

        @DisplayName("대조를 거치지 않은 버전은 정렬되지 않은 것으로 본다.")
        @Test
        void isAligned_dictionaryVersionIsAbsent() {
            // given
            DocumentVersion version = versionOf(null);

            // when & then
            assertThat(version.isAligned(1)).isFalse();
        }

        @DisplayName("기준 사전집 버전이 활성 버전과 다르면 정렬되지 않은 것으로 본다.")
        @Test
        void isAligned_dictionaryVersionDiffers() {
            // given
            DocumentVersion version = versionOf(1);

            // when & then
            assertThat(version.isAligned(2)).isFalse();
        }

        @DisplayName("기준 사전집 버전이 활성 버전과 같으면 정렬된 것으로 본다.")
        @Test
        void isAligned_dictionaryVersionMatches() {
            // given
            DocumentVersion version = versionOf(2);

            // when & then
            assertThat(version.isAligned(2)).isTrue();
        }

        /**
         * 교정 반영 경로를 거치지 않고 정렬 판정만 검증하기 위해 기준 버전을 주입한다.
         */
        private DocumentVersion versionOf(Integer dictionaryVersionNo) {
            DocumentVersion version = DocumentVersion.publishFirst(DOCUMENT_ID, "회원은 결제할 수 있다.", MEMBER_ID);
            ReflectionTestUtils.setField(version, "dictionaryVersionNo", dictionaryVersionNo);

            return version;
        }
    }

    @DisplayName("직접 편집한 버전은 사전집 버전이 같아도 정렬되지 않은 것으로 본다.")
    @Test
    void isAligned_edited() {
        DocumentVersion version = DocumentVersion.publishEdited(
                DOCUMENT_ID, PublishedVersion.initial().next(), "본문", 1, MEMBER_ID);

        assertThat(version.isAligned(1)).isFalse();
    }

    @DisplayName("직접 편집본은 edited다.")
    @Test
    void publishEdited_marksEdited() {
        DocumentVersion version = DocumentVersion.publishEdited(
                DOCUMENT_ID, PublishedVersion.initial().next(), "본문", 1, MEMBER_ID);

        assertThat(version.isEdited()).isTrue();
    }

    @DisplayName("직접 편집본은 이전 사전집 버전을 승계한다.")
    @Test
    void publishEdited_inheritsDictionaryVersionNo() {
        DocumentVersion version = DocumentVersion.publishEdited(
                DOCUMENT_ID, PublishedVersion.initial().next(), "본문", 3, MEMBER_ID);

        assertThat(version.getDictionaryVersionNo()).isEqualTo(3);
    }
}
