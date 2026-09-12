package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.infra.port.DictionaryQueryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentAlignmentReader {
    private final DictionaryQueryPort dictionaryQueryPort;

    public Optional<Integer> activeVersionNo(Long workspaceId) {
        return dictionaryQueryPort.activeVersionNo(workspaceId);
    }

    public boolean readAligned(Long workspaceId, DocumentVersion version) {
        return DocumentVersion.isAligned(
                version.getDictionaryVersionNo(),
                version.isEdited(),
                activeVersionNo(workspaceId).orElse(null));
    }
}
