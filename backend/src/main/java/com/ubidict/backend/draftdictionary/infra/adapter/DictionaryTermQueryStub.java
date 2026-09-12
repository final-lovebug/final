package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 빈 목록을 돌려주는 스텁.
 *
 * <p>사전집이 아직 없는 첫 회차와 같은 입력이 되므로, 스텁 상태에서는 초안이 추출 결과만으로 구성된다(G-1).
 */
@Component("draftDictionaryTermQueryStub")
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class DictionaryTermQueryStub implements DictionaryTermQueryPort {

    @Override
    public List<TermSnapshot> readActiveTerms(Long workspaceId) {
        return List.of();
    }

    @Override
    public Optional<Integer> activeVersionNo(Long workspaceId) {
        return Optional.empty();
    }
}
