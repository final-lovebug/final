package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.infra.port.DraftDocumentQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftDictionaryCreationPolicyValidatorTest {

    @Mock
    private DraftDictionaryRepository draftDictionaryRepository;

    @Mock
    private DraftDocumentQueryPort draftDocumentQueryPort;

    @InjectMocks
    private DraftDictionaryCreationPolicyValidator validator;

    @DisplayName("워크스페이스에 진행 중인 사전 초안이 있으면 새 초안을 만들 수 없다.")
    @Test
    void validate_draftAlreadyExists() {
        // given
        given(draftDictionaryRepository.existsByWorkspaceIdAndStatusNotAndDeletedAtIsNull(
                        1L, DraftDictionaryStatus.REVISED))
                .willReturn(true);

        // when & then
        assertAlreadyExists(() -> validator.validate(1L));
    }

    @DisplayName("워크스페이스에 진행 중인 문서 초안이 있으면 사전 초안을 만들 수 없다.")
    @Test
    void validate_documentDraftAlreadyExists() {
        // given
        given(draftDocumentQueryPort.hasOngoingDraft(1L)).willReturn(true);

        // when & then
        assertAlreadyExists(() -> validator.validate(1L));
    }

    private static void assertAlreadyExists(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXISTS));
    }
}
