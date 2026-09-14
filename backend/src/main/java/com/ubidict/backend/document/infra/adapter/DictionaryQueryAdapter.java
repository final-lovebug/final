package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.document.infra.port.DictionaryQueryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "real")
public class DictionaryQueryAdapter implements DictionaryQueryPort {
    private final DictionaryRepository dictionaryRepository;

    @Override
    public Optional<Integer> activeVersionNo(Long workspaceId) {
        return dictionaryRepository
                .findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE)
                .map(dictionary -> dictionary.versionNo());
    }
}
