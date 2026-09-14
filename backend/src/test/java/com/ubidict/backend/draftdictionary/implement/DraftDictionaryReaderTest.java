package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDictionaryReaderTest {

    @DisplayName("없는 사전 초안을 조회하면 예외가 발생한다.")
    @Test
    void read_notFound() {
        DraftDictionaryRepository draftDictionaryRepository = mock(DraftDictionaryRepository.class);
        given(draftDictionaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> new DraftDictionaryReader(draftDictionaryRepository).read(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_FOUND);
    }
}
