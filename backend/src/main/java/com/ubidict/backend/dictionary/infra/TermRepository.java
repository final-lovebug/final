package com.ubidict.backend.dictionary.infra;

import com.ubidict.backend.dictionary.domain.Term;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 용어는 소속 사전집 버전과 함께 얼어붙어 개별 삭제 경로가 없으므로 조회 조건에 deletedAt을 넣지 않는다.
 *
 * <p>findAllByDictionaryIdIn은 버전 이력 응답의 용어 수를 한 번에 채우는 데 쓴다. 버전마다 세면 N+1이 된다.
 */
public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findAllByDictionaryIdOrderByPreferredFormAsc(Long dictionaryId);

    List<Term> findAllByDictionaryIdIn(Collection<Long> dictionaryIds);
}
