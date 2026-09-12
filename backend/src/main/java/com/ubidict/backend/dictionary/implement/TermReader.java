package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public PageResult<TermPageItem> readPage(Long dictionaryId, DictionarySearchQuery query) {
        Sort.Direction direction = query.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(query.page(), query.size(), Sort.by(direction, query.sortField()));
        var page = query.keyword() == null || query.keyword().isBlank()
                ? termRepository.findSummariesByDictionaryId(dictionaryId, pageRequest)
                : termRepository.findSummariesByKeyword(
                        dictionaryId, query.keyword().strip(), pageRequest);
        return new PageResult<>(
                page.getContent().stream()
                        .map(summary ->
                                new TermPageItem(summary.getId(), summary.getPreferredForm(), summary.getEnglishName()))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }
}
