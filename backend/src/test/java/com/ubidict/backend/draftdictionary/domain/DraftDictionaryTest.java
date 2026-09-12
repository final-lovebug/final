package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDictionaryTest {

    @DisplayName("사전 초안을 생성하면 교정중 상태로 시작한다.")
    @Test
    void create() {
        // when
        DraftDictionary draft = draft();

        // then
        assertThat(draft.getStatus()).isEqualTo(DraftDictionaryStatus.EXAMINING);
        assertThat(draft.getDictionaryId()).isNull();
    }

    @DisplayName("유래 문서가 없으면 사전 초안을 생성할 수 없다.")
    @Test
    void create_sourceDocumentsIsEmpty() {
        // when & then
        assertThatThrownBy(() -> DraftDictionary.create(1L, null, List.of(), 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_SOURCE_DOCUMENT_REQUIRED));
    }

    @DisplayName("교정중인 초안을 교정완료로 전환한다.")
    @Test
    void markExamined() {
        // given
        DraftDictionary draft = draft();

        // when
        draft.markExamined();

        // then
        assertThat(draft.getStatus()).isEqualTo(DraftDictionaryStatus.EXAMINED);
    }

    @DisplayName("교정완료된 초안을 리뷰요청됨으로 전환한다.")
    @Test
    void markReviewRequested() {
        // given
        DraftDictionary draft = draft();
        draft.markExamined();

        // when
        draft.markReviewRequested();

        // then
        assertThat(draft.getStatus()).isEqualTo(DraftDictionaryStatus.REVIEW_REQUESTED);
    }

    @DisplayName("이미 교정완료된 초안을 다시 완료할 수 없다.")
    @Test
    void markExamined_alreadyExamined() {
        // given
        DraftDictionary draft = draft();
        draft.markExamined();

        // when & then
        assertThatThrownBy(draft::markExamined)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXAMINED));
    }

    @DisplayName("교정완료되지 않은 초안으로 리뷰를 요청할 수 없다.")
    @Test
    void markReviewRequested_notExamined() {
        // given
        DraftDictionary draft = draft();

        // when & then
        assertThatThrownBy(draft::markReviewRequested)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINED));
    }

    @DisplayName("이미 리뷰요청된 초안으로 다시 리뷰를 요청할 수 없다.")
    @Test
    void markReviewRequested_alreadyRequested() {
        // given
        DraftDictionary draft = draft();
        draft.markExamined();
        draft.markReviewRequested();

        // when & then
        assertThatThrownBy(draft::markReviewRequested)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_REVIEW_REQUESTED));
    }

    private DraftDictionary draft() {
        return DraftDictionary.create(1L, null, List.of(10L), 2L);
    }
}
