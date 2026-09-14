package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDocumentReaderTest {

    private final DraftDocumentRepository draftDocumentRepository = mock(DraftDocumentRepository.class);
    private final DraftDocumentReader draftDocumentReader = new DraftDocumentReader(draftDocumentRepository);

    @DisplayName("존재하지 않는 문서 초안을 조회하면 예외가 발생한다.")
    @Test
    void read_notFound() {
        // given
        given(draftDocumentRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> draftDocumentReader.read(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND));
    }
}
