package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.reviewrequest.infra.port.DocumentVersionPublishPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 리뷰 도메인의 승인 결과를 문서 서비스의 발행 트랜잭션에 위임한다. */
@Component
@ConditionalOnProperty(name = "app.crossdomain.document-publish.mode", havingValue = "real")
@RequiredArgsConstructor
public class DocumentVersionPublishAdapter implements DocumentVersionPublishPort {

    private final DocumentService documentService;

    @Override
    public int publish(Long documentId, int baseVersionNo, String body, int dictionaryVersionNo, Long publishedBy) {
        return documentService.publishRevised(documentId, baseVersionNo, body, dictionaryVersionNo, publishedBy);
    }
}
