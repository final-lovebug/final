package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.infra.port.WorkspacePolicyPort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentAccessValidatorTest {

    private final DocumentQueryPort documentQueryPort = mock(DocumentQueryPort.class);
    private final WorkspacePolicyPort workspacePolicyPort = mock(WorkspacePolicyPort.class);
    private final DraftDocumentAccessValidator validator =
            new DraftDocumentAccessValidator(documentQueryPort, workspacePolicyPort);

    @DisplayName("워크스페이스 비참여자가 초안을 생성하려 하면 문서를 찾을 수 없는 것으로 응답한다.")
    @Test
    void validateCreation_notParticipant() {
        given(documentQueryPort.read(10L)).willReturn(Optional.of(new DocumentSnapshot(10L, 20L, 1, "본문")));
        given(workspacePolicyPort.isParticipant(20L, 30L)).willReturn(false);

        assertThatThrownBy(() -> validator.validateCreation(10L, 30L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND));
    }
}
