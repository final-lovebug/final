package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DictionaryAppender {

    private final DictionaryRepository dictionaryRepository;

    public Dictionary appendFirst(Long workspaceId, Long createdBy) {
        return dictionaryRepository.save(Dictionary.createFirst(workspaceId, createdBy));
    }

    /**
     * 이전 버전은 보관 처리된 뒤에 들어와야 한다. 활성 사전집이 둘이 되는 순간 유니크 제약에 걸린다.
     */
    public Dictionary appendNext(Dictionary previous, Long createdBy) {
        return dictionaryRepository.save(Dictionary.nextVersion(previous, createdBy));
    }
}
