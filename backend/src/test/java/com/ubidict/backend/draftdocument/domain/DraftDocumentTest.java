package com.ubidict.backend.draftdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentTest {

    private static final Long DOCUMENT_ID = 10L;
    private static final Long MEMBER_ID = 1L;

    @DisplayName("초안을 생성하면 교정중 상태로 시작한다.")
    @Test
    void create() {
        // when
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "회원은 결제할 수 있다.", MEMBER_ID, MEMBER_ID);

        // then
        assertThat(draftDocument.getStatus()).isEqualTo(DraftDocumentStatus.EXAMINING);
        assertThat(draftDocument.getRequestedBy()).isEqualTo(MEMBER_ID);
        assertThat(draftDocument.getCreatedBy()).isEqualTo(MEMBER_ID);
        assertThat(draftDocument.getDictionaryVersionNo()).isEqualTo(1);
    }

    @DisplayName("대조 기준 사전집 버전이 없으면 예외가 발생한다 — 기준 없이 만들어진 초안은 발행할 버전을 정할 수 없다(D-93).")
    @Test
    void create_dictionaryVersionIsMissing() {
        // when & then
        assertThatThrownBy(() -> DraftDocument.create(DOCUMENT_ID, 1, null, "본문", MEMBER_ID, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_DICTIONARY_VERSION));
    }

    @DisplayName("초안 본문의 앞뒤 공백을 제거한다.")
    @Test
    void create_bodyIsTrimmed() {
        // when
        DraftDocument draftDocument =
                DraftDocument.create(DOCUMENT_ID, 1, 1, "  회원은 결제할 수 있다.  ", MEMBER_ID, MEMBER_ID);

        // then
        assertThat(draftDocument.getDraftBody()).isEqualTo("회원은 결제할 수 있다.");
    }

    @DisplayName("초안 본문이 비어 있으면 예외가 발생한다.")
    @Test
    void create_bodyIsBlank() {
        // when & then
        assertThatThrownBy(() -> DraftDocument.create(DOCUMENT_ID, 1, 1, "   ", MEMBER_ID, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_BODY));
    }

    @DisplayName("기준 문서 버전이 1보다 작으면 예외가 발생한다.")
    @Test
    void create_baseVersionIsInvalid() {
        // when & then
        assertThatThrownBy(() -> DraftDocument.create(DOCUMENT_ID, 0, 1, "본문", MEMBER_ID, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_BASE_VERSION));
    }

    @DisplayName("교정을 완료하면 조립한 본문을 확정한다.")
    @Test
    void markExamined() {
        // given
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "회원은 결제할 수 있다.", MEMBER_ID, MEMBER_ID);

        // when
        draftDocument.markExamined("사용자는 결제할 수 있다.");

        // then
        assertThat(draftDocument.getStatus()).isEqualTo(DraftDocumentStatus.EXAMINED);
        assertThat(draftDocument.getDraftBody()).isEqualTo("사용자는 결제할 수 있다.");
    }

    @DisplayName("이미 교정완료된 초안은 다시 완료할 수 없다.")
    @Test
    void markExamined_alreadyExamined() {
        // given
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "회원은 결제할 수 있다.", MEMBER_ID, MEMBER_ID);
        draftDocument.markExamined("사용자는 결제할 수 있다.");

        // when & then
        assertThatThrownBy(() -> draftDocument.markExamined("다시 고친 본문"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXAMINED));
    }

    @DisplayName("리뷰가 취소되면 교정완료 상태로 돌아간다.")
    @Test
    void reopen_returnsToExamined() {
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "본문", MEMBER_ID, MEMBER_ID);
        draftDocument.markExamined("본문");
        draftDocument.markReviewRequested();

        draftDocument.reopen();

        assertThat(draftDocument.getStatus()).isEqualTo(DraftDocumentStatus.EXAMINED);
    }

    @DisplayName("반영완료된 초안은 상태를 바꿀 수 없다.")
    @Test
    void markRevised_isTerminal() {
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "본문", MEMBER_ID, MEMBER_ID);
        draftDocument.markExamined("본문");
        draftDocument.markReviewRequested();
        draftDocument.markRevised();

        assertThatThrownBy(draftDocument::reopen)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_STATUS_TRANSITION));
    }

    @DisplayName("교정 완료 전에는 리뷰 요청 자격이 없다.")
    @Test
    void validateExaminedForReview_isExamining() {
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "본문", MEMBER_ID, MEMBER_ID);

        assertThatThrownBy(draftDocument::validateExaminedForReview)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_STATUS_TRANSITION));
    }

    @DisplayName("같은 리뷰 요청 이벤트를 두 번 받아도 상태는 한 번만 변경된다.")
    @Test
    void markReviewRequested_isIdempotent() {
        DraftDocument draftDocument = DraftDocument.create(DOCUMENT_ID, 1, 1, "본문", MEMBER_ID, MEMBER_ID);
        draftDocument.markExamined("본문");

        draftDocument.markReviewRequested();
        draftDocument.markReviewRequested();

        assertThat(draftDocument.getStatus()).isEqualTo(DraftDocumentStatus.REVIEW_REQUESTED);
    }
}
