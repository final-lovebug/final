package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.infra.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentAppender {

    private final DocumentRepository documentRepository;

    public Document append(Long workspaceId, String title, Long memberId) {
        return documentRepository.save(Document.create(workspaceId, title, memberId));
    }
}
