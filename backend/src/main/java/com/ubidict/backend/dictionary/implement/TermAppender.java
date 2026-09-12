package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.infra.TermRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TermAppender {

    private final TermRepository termRepository;

    /**
     * 새 버전의 용어를 통째로 저장한다. 이전 버전에서 복사하지 않고 넘어온 목록만 그 버전의 내용이 된다.
     *
     * <p>빈 사전집 버전은 대조에 쓸 수 없으므로 용어가 없으면 만들지 않는다.
     *
     * <p>저장 결과를 돌려주지 않는다. 응답에 실을 용어는 정렬이 필요해 TermReader가 다시 읽는다 — 정렬 기준을 조회 쿼리 한 곳에만 둔다.
     */
    public void appendAll(Long dictionaryId, List<NewTerm> newTerms, Long createdBy) {
        if (newTerms.isEmpty()) {
            throw new BusinessException(DictionaryErrorCode.DICTIONARY_EMPTY_TERMS);
        }
        List<Term> terms = newTerms.stream()
                .map(newTerm -> newTerm.toTerm(dictionaryId, createdBy))
                .toList();

        termRepository.saveAll(terms);
    }
}
