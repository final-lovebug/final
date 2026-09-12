package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.draftdictionary.infra.port.DocumentQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 어떤 문서도 추출 대상이 아니라고 답하는 스텁.
 *
 * <p>거짓을 돌려준다 — 폐기된 D-15의 {@code isOutdated} 스텁이 거짓이던 것과 값은 같지만 의미가 반대다(R-9).
 * 그때는 「낡지 않았다」였고 지금은 「대상이 아니다」다.
 */
@Component("documentQueryStubForDraftDictionary")
@ConditionalOnProperty(name = "app.crossdomain.document.mode", havingValue = "stub", matchIfMissing = true)
public class DocumentQueryStub implements DocumentQueryPort {

    @Override
    public boolean isExtractable(Long documentId) {
        return false;
    }
}
