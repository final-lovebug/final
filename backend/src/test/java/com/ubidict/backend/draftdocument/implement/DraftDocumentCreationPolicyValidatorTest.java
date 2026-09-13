package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentCreationPolicyValidatorTest {

    private static final Long DOCUMENT_ID = 10L;
    private static final Long WORKSPACE_ID = 20L;

    private final DraftDocumentRepository draftDocumentRepository = mock(DraftDocumentRepository.class);
    private final ReviewRequestQueryPort reviewRequestQueryPort = mock(ReviewRequestQueryPort.class);
    private final DraftDictionaryQueryPort draftDictionaryQueryPort = mock(DraftDictionaryQueryPort.class);
    private final DraftDocumentCreationPolicyValidator validator = new DraftDocumentCreationPolicyValidator(
            draftDocumentRepository, reviewRequestQueryPort, draftDictionaryQueryPort);

    @DisplayName("같은 문서에 진행 중인 초안이 있으면 새 초안을 만들 수 없다.")
    @Test
    void validate_draftAlreadyExists() {
        given(draftDocumentRepository.existsByDocumentIdAndStatusNotAndDeletedAtIsNull(
                        DOCUMENT_ID, DraftDocumentStatus.REVISED))
                .willReturn(true);

        assertThatThrownBy(() -> validator.validate(DOCUMENT_ID, WORKSPACE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXISTS));
    }

    @DisplayName("문서 리뷰가 진행 중이면 새 초안을 만들 수 없다.")
    @Test
    void validate_documentIsUnderReview() {
        given(reviewRequestQueryPort.hasOngoingDocumentReview(DOCUMENT_ID)).willReturn(true);

        assertThatThrownBy(() -> validator.validate(DOCUMENT_ID, WORKSPACE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_UNDER_REVIEW));
    }

    @DisplayName("워크스페이스에 사전 초안이 진행 중이면 문서 초안을 만들 수 없다.")
    @Test
    void validate_draftDictionaryExists() {
        given(draftDictionaryQueryPort.hasOngoingDraft(WORKSPACE_ID)).willReturn(true);

        assertThatThrownBy(() -> validator.validate(DOCUMENT_ID, WORKSPACE_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXISTS));
    }
}
