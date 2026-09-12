package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 문서를 찾지 못한 것으로 답하는 스텁.
 *
 * <p>본문을 서버가 채우는 경로가 열리기 전에는 클라이언트가 draftBody를 실어 보내므로 이 값이 쓰이지 않는다(D-36).
 */
@Component("documentQueryStubForDraftDocument")
@ConditionalOnProperty(name = "app.crossdomain.document.mode", havingValue = "stub", matchIfMissing = true)
public class DocumentQueryStub implements DocumentQueryPort {

    @Override
    public Optional<DocumentSnapshot> read(Long documentId) {
        return Optional.empty();
    }
}
