package com.ubidict.backend.document.implement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.document.infra.port.DraftDocumentQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentEditGuardTest {
    @Mock
    private DraftDocumentQueryPort draftDocumentQueryPort;

    @Mock
    private DraftDictionaryQueryPort draftDictionaryQueryPort;

    @Test
    @DisplayName("진행 중인 문서 초안이 있으면 본문 편집을 거절한다.")
    void validate_ongoingDocumentDraftExists() {
        given(draftDocumentQueryPort.hasOngoingDraft(1L)).willReturn(true);

        assertThatThrownBy(() -> guard().validate(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_DRAFT_IN_PROGRESS);
    }

    @Test
    @DisplayName("진행 중인 사전 초안의 원천 문서이면 본문 편집을 거절한다.")
    void validate_sourceOfOngoingDictionaryDraft() {
        given(draftDictionaryQueryPort.isSourceOfOngoingDraft(1L)).willReturn(true);

        assertThatThrownBy(() -> guard().validate(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT);
    }

    @Test
    @DisplayName("진행 중인 초안이 없으면 본문 편집을 허용한다.")
    void validate_noDraft() {
        guard().validate(1L);
    }

    private DocumentEditGuard guard() {
        return new DocumentEditGuard(draftDocumentQueryPort, draftDictionaryQueryPort);
    }
}
