package com.ubidict.backend.document.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.infra.port.DictionaryQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentAlignmentReaderTest {
    @Mock
    private DictionaryQueryPort dictionaryQueryPort;

    @Test
    @DisplayName("활성 사전집이 없으면 정렬된 것으로 읽는다.")
    void readAligned_dictionaryIsAbsent() {
        given(dictionaryQueryPort.activeVersionNo(1L)).willReturn(Optional.empty());

        assertThat(new DocumentAlignmentReader(dictionaryQueryPort)
                        .readAligned(1L, DocumentVersion.publishFirst(1L, "본문", 1L)))
                .isTrue();
    }

    @Test
    @DisplayName("직접 편집본은 사전집 버전이 같아도 정렬되지 않은 것으로 읽는다.")
    void readAligned_edited() {
        given(dictionaryQueryPort.activeVersionNo(1L)).willReturn(Optional.of(1));
        DocumentVersion version = DocumentVersion.publishEdited(
                1L,
                com.ubidict.backend.document.domain.PublishedVersion.initial().next(),
                "본문",
                1,
                1L);

        assertThat(new DocumentAlignmentReader(dictionaryQueryPort).readAligned(1L, version))
                .isFalse();
    }
}
