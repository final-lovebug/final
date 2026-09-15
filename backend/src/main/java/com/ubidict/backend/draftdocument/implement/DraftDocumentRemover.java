package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import org.springframework.stereotype.Component;

@Component
public class DraftDocumentRemover {

    public void remove(DraftDocument draftDocument) {
        draftDocument.delete();
    }
}
