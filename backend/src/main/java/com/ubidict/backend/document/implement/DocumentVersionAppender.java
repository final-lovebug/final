package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentVersionAppender {

    private final DocumentVersionRepository documentVersionRepository;

    /**
     * 업로드본을 v1으로 발행한다. 문서에는 본문이 없으므로 이 호출이 빠지면 본문 없는 문서가 남는다.
     */
    public DocumentVersion appendFirst(Document document, String body, Long memberId) {
        return documentVersionRepository.save(DocumentVersion.publishFirst(document.getId(), body, memberId));
    }

    public DocumentVersion appendEdited(Document document, DocumentVersion previous, String body, Long memberId) {
        return documentVersionRepository.save(DocumentVersion.publishEdited(
                document.getId(), previous.getVersion().next(), body, previous.getDictionaryVersionNo(), memberId));
    }

    public DocumentVersion appendRevised(
            Document document, DocumentVersion previous, String body, int dictionaryVersionNo, Long memberId) {
        return documentVersionRepository.save(DocumentVersion.publishRevised(
                document.getId(), previous.getVersion().next(), body, dictionaryVersionNo, memberId));
    }
}
