package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.TermSnapshot;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 빈 목록을 돌려주는 스텁.
 *
 * <p>표준어를 찾을 수 없으므로 대조가 제안어를 만들지 못한다 — 사전집이 없는 기간과 같은 상태다.
 */
@Component("draftDocumentTermQueryStub")
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class DictionaryTermQueryStub implements DictionaryTermQueryPort {

    @Override
    public List<TermSnapshot> readActiveTerms(Long workspaceId) {
        return List.of();
    }
}
