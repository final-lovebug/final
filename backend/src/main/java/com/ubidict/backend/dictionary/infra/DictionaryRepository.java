package com.ubidict.backend.dictionary.infra;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사전집은 소프트 삭제를 쓰지 않으므로 workspace·member와 달리 조회 조건에 deletedAt을 넣지 않는다. 모든 행이 보존해야 할 버전 이력이다.
 *
 * <p>버전 번호는 임베디드 값 객체 안에 있어 파생 쿼리 이름이 VersionVersionNo가 된다(version 안의 versionNo).
 */
public interface DictionaryRepository extends JpaRepository<Dictionary, Long> {

    Optional<Dictionary> findByWorkspaceIdAndStatus(Long workspaceId, DictionaryStatus status);

    Optional<Dictionary> findByWorkspaceIdAndVersionVersionNo(Long workspaceId, int versionNo);

    List<Dictionary> findAllByWorkspaceIdOrderByVersionVersionNoDesc(Long workspaceId);
}
