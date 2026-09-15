package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDocumentWriter {

    private final DraftDocumentRepository draftDocumentRepository;

    public DraftDocument append(
            Long documentId, int baseVersionNo, String draftBody, Long requestedBy, Long createdBy) {
        DraftDocument draftDocument =
                DraftDocument.create(documentId, baseVersionNo, draftBody, requestedBy, createdBy);

        return draftDocumentRepository.save(draftDocument);
    }

    public void updateBody(DraftDocument draftDocument, String draftBody) {
        draftDocument.updateBody(draftBody);
    }
}
