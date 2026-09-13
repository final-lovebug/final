package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.reviewrequest.infra.port.ActiveDictionaryVersionQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "real")
public class ActiveDictionaryVersionQueryAdapter implements ActiveDictionaryVersionQueryPort {

    private final DictionaryRepository dictionaryRepository;

    @Override
    public int activeVersionNo(Long workspaceId) {
        return dictionaryRepository
                .findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(DictionaryErrorCode.DICTIONARY_NOT_FOUND))
                .versionNo();
    }

    @Override
    public int baseVersionNoForNextVersion(Long workspaceId) {
        return dictionaryRepository
                .findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE)
                .map(Dictionary::versionNo)
                .orElse(0);
    }
}
