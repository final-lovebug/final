package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDictionaryTest {

    @DisplayName("사전 초안을 생성하면 교정중 상태로 시작합니다.")
    @Test
    void create() {
        DraftDictionary draftDictionary = DraftDictionary.create(1L, null, List.of(10L), 2L);

        assertThat(draftDictionary.getStatus()).isEqualTo(DraftDictionaryStatus.EXAMINING);
        assertThat(draftDictionary.getDictionaryId()).isNull();
    }

    @DisplayName("유래 문서가 없으면 사전 초안을 생성할 수 없습니다.")
    @Test
    void create_sourceDocumentsIsEmpty() {
        assertThatThrownBy(() -> DraftDictionary.create(1L, null, List.of(), 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_SOURCE_DOCUMENT_REQUIRED);
    }

    @DisplayName("유래 문서가 중복되면 사전 초안을 생성할 수 없습니다.")
    @Test
    void create_sourceDocumentsAreDuplicated() {
        assertThatThrownBy(() -> DraftDictionary.create(1L, null, List.of(10L, 10L), 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT);
    }

    @DisplayName("유래 문서를 변경하면 목록 전체가 교체됩니다.")
    @Test
    void replaceSourceDocuments() {
        DraftDictionary draftDictionary = DraftDictionary.create(1L, null, List.of(10L), 2L);

        draftDictionary.replaceSourceDocuments(List.of(20L, 30L));

        assertThat(draftDictionary.getSourceDocumentIds()).containsExactly(20L, 30L);
    }
}
