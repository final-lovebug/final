package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionDocumentWriter {
    private final RevisionDocumentRepository repository;

    public RevisionDocument write(RevisionDocument revision) {
        return repository.save(revision);
    }
}
