package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.reviewrequest.infra.port.DocumentVersionPublishPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 발행 경로가 아직 연결되지 않았음을 알리는 스텁.
 *
 * <p><b>조회 스텁과 달리 값을 돌려주지 않고 예외를 던진다.</b> 조회에서 빈 목록·false·빈 Optional은 「찾지 못했다」는 정직한 답이지만,
 * 발행에는 그런 답이 없다. 버전 번호를 지어내면 리뷰 요청이 존재하지 않는 버전을 결과로 기록하고 그대로 넘어간다.
 *
 * <p>real 어댑터는 제공 도메인이 넣는다 — DOC-6.
 */
@Component
@ConditionalOnProperty(name = "app.crossdomain.document-publish.mode", havingValue = "stub", matchIfMissing = true)
public class DocumentVersionPublishStub implements DocumentVersionPublishPort {

    @Override
    public int publish(Long documentId, int baseVersionNo, String body, int dictionaryVersionNo) {
        throw new UnsupportedOperationException(
                "문서 버전 발행 어댑터가 아직 없다. DOC-6을 머지하고 app.crossdomain.document-publish.mode를 real로 바꾼다.");
    }
}
