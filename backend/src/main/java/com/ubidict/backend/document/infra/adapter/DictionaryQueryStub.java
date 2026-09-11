package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.document.infra.port.DictionaryQueryPort;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class DictionaryQueryStub implements DictionaryQueryPort {
    @Override
    public Optional<Integer> activeVersionNo(Long workspaceId) {
        return Optional.empty();
    }
}
