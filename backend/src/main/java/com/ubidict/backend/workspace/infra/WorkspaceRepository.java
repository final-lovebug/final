package com.ubidict.backend.workspace.infra;

import com.ubidict.backend.workspace.domain.Workspace;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 소프트 삭제 조건을 메서드 이름에 박아 조회에서 빠뜨릴 수 없게 한다.
 *
 * <p>전역 @SQLRestriction을 쓰지 않는 이유는, 삭제된 행을 조회해야 하는 유스케이스(복구·감사)가 생겼을 때 우회가 어려워지기 때문이다.
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findByIdAndDeletedAtIsNull(Long id);

    List<Workspace> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);
}
