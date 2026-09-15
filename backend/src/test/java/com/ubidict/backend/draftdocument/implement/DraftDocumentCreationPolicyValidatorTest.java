package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentCreationPolicyValidatorTest {

    private static final Long DOCUMENT_ID = 10L;

    private final DraftDocumentRepository draftDocumentRepository = mock(DraftDocumentRepository.class);
    private final ReviewRequestQueryPort reviewRequestQueryPort = mock(ReviewRequestQueryPort.class);
    private final DraftDocumentCreationPolicyValidator validator =
            new DraftDocumentCreationPolicyValidator(draftDocumentRepository, reviewRequestQueryPort);

    @DisplayName("같은 문서에 진행 중인 초안이 있으면 새 초안을 만들 수 없다.")
    @Test
    void validate_draftAlreadyExists() {
        given(draftDocumentRepository.existsByDocumentIdAndStatusNotAndDeletedAtIsNull(
                        DOCUMENT_ID, DraftDocumentStatus.REVISED))
                .willReturn(true);

        assertThatThrownBy(() -> validator.validate(DOCUMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXISTS));
    }

    @DisplayName("문서 리뷰가 진행 중이면 새 초안을 만들 수 없다.")
    @Test
    void validate_documentIsUnderReview() {
        given(reviewRequestQueryPort.hasOngoingDocumentReview(DOCUMENT_ID)).willReturn(true);

        assertThatThrownBy(() -> validator.validate(DOCUMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_UNDER_REVIEW));
    }

    @DisplayName("그 문서에 걸린 것이 없으면 통과한다 — 워크스페이스의 사전집 초안은 더 이상 보지 않는다(D-93).")
    @Test
    void validate_pass() {
        assertThatCode(() -> validator.validate(DOCUMENT_ID)).doesNotThrowAnyException();
    }
}
