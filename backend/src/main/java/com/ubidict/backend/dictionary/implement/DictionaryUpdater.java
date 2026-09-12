package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DictionaryUpdater {

    private final DictionaryRepository dictionaryRepository;

    /**
     * 지나간 버전으로 내린다.
     *
     * <p>여기서 바로 flush하는 이유가 있다. Hibernate는 한 트랜잭션의 쓰기를 모을 때 INSERT를 UPDATE보다 먼저 실행한다. 보관 전환을 미뤄 두면 새 버전
     * INSERT가 먼저 나가면서 활성 사전집이 순간 둘이 되어 uk_dictionary_workspace_active에 걸린다.
     */
    public void archive(Dictionary dictionary) {
        dictionary.archive();
        dictionaryRepository.saveAndFlush(dictionary);
    }
}
