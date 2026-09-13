package com.ubidict.backend.revisionlog.infra;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionLogRepository extends JpaRepository<RevisionLog, Long> {

    Page<RevisionLog> findAllByWorkspaceIdAndTargetTypeAndTargetId(
            Long workspaceId, RevisionLogTargetType targetType, Long targetId, Pageable pageable);
}
