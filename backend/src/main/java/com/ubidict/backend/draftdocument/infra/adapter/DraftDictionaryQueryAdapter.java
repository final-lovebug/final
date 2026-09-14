package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdocument.infra.port.DraftDictionaryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("draftDictionaryQueryAdapterForDraftDocument")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "real")
public class DraftDictionaryQueryAdapter implements DraftDictionaryQueryPort {
    private final DraftDictionaryRepository draftDictionaryRepository;

    @Override
    public boolean hasOngoingDraft(Long workspaceId) {
        return draftDictionaryRepository.existsByWorkspaceIdAndStatusNotAndDeletedAtIsNull(
                workspaceId, DraftDictionaryStatus.REVISED);
    }
}
