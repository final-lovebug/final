package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.reviewrequest.infra.port.ActiveDictionaryVersionQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class ActiveDictionaryVersionQueryStub implements ActiveDictionaryVersionQueryPort {

    @Override
    public int activeVersionNo(Long workspaceId) {
        throw new UnsupportedOperationException("활성 사전집 버전 조회 어댑터가 연결되지 않았습니다.");
    }

    @Override
    public int baseVersionNoForNextVersion(Long workspaceId) {
        throw new UnsupportedOperationException("활성 사전집 버전 조회 어댑터가 연결되지 않았습니다.");
    }
}
