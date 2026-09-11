package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDocumentServiceTest extends IntegrationTestSupport {

    private static final Long DOCUMENT_ID = 10L;
    private static final Long MEMBER_ID = 1L;

    @Autowired
    private DraftDocumentService draftDocumentService;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @DisplayName("문서 초안을 생성한다.")
    @Test
    void create() {
        // when
        DraftDocumentResult result = createDraft("회원은 결제할 수 있다.");

        // then
        assertThat(result.draftDocumentId()).isNotNull();
        assertThat(result.documentId()).isEqualTo(DOCUMENT_ID);
        assertThat(result.status()).isEqualTo(DraftDocumentStatus.EXAMINING);
        assertThat(result.requestedBy()).isEqualTo(MEMBER_ID);
    }

    @DisplayName("문서 초안의 본문을 수정한다.")
    @Test
    void updateBody() {
        // given
        DraftDocumentResult created = createDraft("기존 본문");

        // when
        DraftDocumentResult result = draftDocumentService.updateBody(
                new UpdateDraftBodyCommand(created.draftDocumentId(), "수정 본문", MEMBER_ID));

        // then
        assertThat(result.draftBody()).isEqualTo("수정 본문");
        assertThat(draftDocumentService
                        .read(created.draftDocumentId(), MEMBER_ID)
                        .draftBody())
                .isEqualTo("수정 본문");
    }

    @DisplayName("존재하지 않는 문서 초안의 본문을 수정하면 예외가 발생한다.")
    @Test
    void updateBody_draftDocumentNotFound() {
        // when & then
        assertThatThrownBy(() -> draftDocumentService.updateBody(new UpdateDraftBodyCommand(999L, "수정 본문", MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND));
    }

    @DisplayName("문서 초안을 삭제하면 이후 조회되지 않는다.")
    @Test
    void delete() {
        // given
        DraftDocumentResult created = createDraft("삭제할 본문");

        // when
        draftDocumentService.delete(created.draftDocumentId(), MEMBER_ID);

        // then
        assertThat(draftDocumentRepository.findByIdAndDeletedAtIsNull(created.draftDocumentId()))
                .isEmpty();
        assertThat(draftDocumentRepository.findById(created.draftDocumentId()))
                .get()
                .extracting(DraftDocument::isDeleted)
                .isEqualTo(true);
    }

    private DraftDocumentResult createDraft(String draftBody) {
        return draftDocumentService.create(new CreateDraftDocumentCommand(DOCUMENT_ID, 1, draftBody, MEMBER_ID));
    }
}
