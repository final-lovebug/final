package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.infra.TermRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TermReader {

    private final TermRepository termRepository;

    public List<Term> readAll(Long dictionaryId) {
        return termRepository.findAllByDictionaryIdOrderByPreferredFormAsc(dictionaryId);
    }

    /**
     * 버전 이력 응답의 용어 수를 한 번에 채운다. 버전마다 세면 N+1이 된다.
     *
     * <p>용어가 하나도 없는 사전집은 결과에 나타나지 않으므로 호출자가 기본값 0을 채운다.
     */
    public Map<Long, Long> countByDictionaryIds(Collection<Long> dictionaryIds) {
        if (dictionaryIds.isEmpty()) {
            return Map.of();
        }

        return termRepository.findAllByDictionaryIdIn(dictionaryIds).stream()
                .collect(Collectors.groupingBy(Term::getDictionaryId, Collectors.counting()));
    }
}
