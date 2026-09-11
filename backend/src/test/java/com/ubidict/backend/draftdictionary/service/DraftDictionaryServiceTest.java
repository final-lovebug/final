package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.service.model.CreateDraftDictionaryCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.draftdictionary.service.model.UpdateSourceDocumentsCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDictionaryServiceTest extends IntegrationTestSupport {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Autowired
    private DraftDictionaryService draftDictionaryService;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @DisplayName("사전집이 없는 첫 회차의 사전 초안을 생성한다.")
    @Test
    void create() {
        DraftDictionaryResult result = createDraft(List.of(10L));

        assertThat(result.draftDictionaryId()).isNotNull();
        assertThat(result.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.dictionaryId()).isNull();
        assertThat(result.status()).isEqualTo(DraftDictionaryStatus.EXAMINING);
        assertThat(result.createdBy()).isEqualTo(MEMBER_ID);
    }

    @DisplayName("사전 초안의 유래 문서 목록을 교체한다.")
    @Test
    void updateSourceDocuments() {
        DraftDictionaryResult created = createDraft(List.of(10L));

        DraftDictionaryResult updated = draftDictionaryService.updateSourceDocuments(
                new UpdateSourceDocumentsCommand(created.draftDictionaryId(), List.of(20L, 30L), MEMBER_ID));

        assertThat(updated.sourceDocumentIds()).containsExactly(20L, 30L);
        assertThat(draftDictionaryService
                        .read(created.draftDictionaryId(), MEMBER_ID)
                        .sourceDocumentIds())
                .containsExactlyInAnyOrder(20L, 30L);
    }

    @DisplayName("사전 초안을 삭제하면 이후 조회되지 않는다.")
    @Test
    void delete() {
        DraftDictionaryResult created = createDraft(List.of(10L));

        draftDictionaryService.delete(created.draftDictionaryId(), MEMBER_ID);

        assertThat(draftDictionaryRepository.findByIdAndDeletedAtIsNull(created.draftDictionaryId()))
                .isEmpty();
    }

    @DisplayName("중복된 유래 문서로 사전 초안을 생성할 수 없다.")
    @Test
    void create_duplicateSourceDocument() {
        assertThatThrownBy(() -> createDraft(List.of(10L, 10L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT);
    }

    private DraftDictionaryResult createDraft(List<Long> sourceDocumentIds) {
        return draftDictionaryService.create(
                new CreateDraftDictionaryCommand(WORKSPACE_ID, null, sourceDocumentIds, MEMBER_ID));
    }
}
