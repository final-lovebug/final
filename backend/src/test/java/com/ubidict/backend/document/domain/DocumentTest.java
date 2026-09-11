package com.ubidict.backend.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long OTHER_WORKSPACE_ID = 2L;
    private static final Long MEMBER_ID = 7L;
    private static final Long OTHER_MEMBER_ID = 8L;

    @DisplayName("문서를 만들면 현재 버전이 1이다.")
    @Test
    void create() {
        // when
        Document document = Document.create(WORKSPACE_ID, "결제 도메인 설계", MEMBER_ID);

        // then
        assertThat(document.getCurrentVersionNo()).isEqualTo(1);
        assertThat(document.getTitle()).isEqualTo("결제 도메인 설계");
    }

    @DisplayName("문서를 만들면 최종수정자가 생성자로 초기화된다.")
    @Test
    void create_updaterIsCreator() {
        // when
        Document document = Document.create(WORKSPACE_ID, "결제 도메인 설계", MEMBER_ID);

        // then
        assertThat(document.getCreatedBy()).isEqualTo(MEMBER_ID);
        assertThat(document.getUpdaterId()).isEqualTo(MEMBER_ID);
    }

    @DisplayName("제목의 앞뒤 공백을 제거한다.")
    @Test
    void create_titleIsTrimmed() {
        // when
        Document document = Document.create(WORKSPACE_ID, "  결제 도메인 설계  ", MEMBER_ID);

        // then
        assertThat(document.getTitle()).isEqualTo("결제 도메인 설계");
    }

    @DisplayName("제목이 비어 있으면 예외가 발생한다.")
    @Test
    void create_titleIsBlank() {
        // when & then
        assertThatThrownBy(() -> Document.create(WORKSPACE_ID, "   ", MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_TITLE);
    }

    @DisplayName("제목이 200자를 넘으면 예외가 발생한다.")
    @Test
    void create_titleIsTooLong() {
        // given
        String tooLong = "가".repeat(Document.TITLE_MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Document.create(WORKSPACE_ID, tooLong, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_TITLE);
    }

    @DisplayName("제목이 정확히 200자면 예외가 발생하지 않는다.")
    @Test
    void create_titleIsAtMaxLength() {
        // given
        String maxLength = "가".repeat(Document.TITLE_MAX_LENGTH);

        // when
        Document document = Document.create(WORKSPACE_ID, maxLength, MEMBER_ID);

        // then
        assertThat(document.getTitle()).hasSize(Document.TITLE_MAX_LENGTH);
    }

    @DisplayName("제목을 바꾸면 최종수정자가 갱신된다.")
    @Test
    void rename() {
        // given
        Document document = Document.create(WORKSPACE_ID, "결제 도메인 설계", MEMBER_ID);

        // when
        document.rename("정산 도메인 설계", OTHER_MEMBER_ID);

        // then
        assertThat(document.getTitle()).isEqualTo("정산 도메인 설계");
        assertThat(document.getUpdaterId()).isEqualTo(OTHER_MEMBER_ID);
    }

    @DisplayName("제목을 빈 값으로 바꾸면 예외가 발생한다.")
    @Test
    void rename_titleIsBlank() {
        // given
        Document document = Document.create(WORKSPACE_ID, "결제 도메인 설계", MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> document.rename("", OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_TITLE);
    }

    @DisplayName("라벨만 바뀌어도 최종수정자가 갱신된다.")
    @Test
    void touch() {
        // given
        Document document = Document.create(WORKSPACE_ID, "결제 도메인 설계", MEMBER_ID);

        // when
        document.touch(OTHER_MEMBER_ID);

        // then
        assertThat(document.getUpdaterId()).isEqualTo(OTHER_MEMBER_ID);
    }

    @DisplayName("다음 버전을 발행하면 현재 버전이 증가한다.")
    @Test
    void publishNext_increasesVersionNo() {
        Document document = Document.create(WORKSPACE_ID, "제목", MEMBER_ID);

        document.publishNext(OTHER_MEMBER_ID);

        assertThat(document.getCurrentVersionNo()).isEqualTo(2);
    }

    @DisplayName("다음 버전을 발행하면 최종수정자가 갱신된다.")
    @Test
    void publishNext_updatesUpdaterId() {
        Document document = Document.create(WORKSPACE_ID, "제목", MEMBER_ID);

        document.publishNext(OTHER_MEMBER_ID);

        assertThat(document.getUpdaterId()).isEqualTo(OTHER_MEMBER_ID);
    }

    @DisplayName("다른 워크스페이스의 문서인지 판별한다.")
    @Test
    void belongsTo() {
        // given
        Document document = Document.create(WORKSPACE_ID, "결제 도메인 설계", MEMBER_ID);

        // when & then
        assertThat(document.belongsTo(WORKSPACE_ID)).isTrue();
        assertThat(document.belongsTo(OTHER_WORKSPACE_ID)).isFalse();
    }
}
