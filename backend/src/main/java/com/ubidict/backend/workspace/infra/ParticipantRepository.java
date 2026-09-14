package com.ubidict.backend.workspace.infra;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(Long workspaceId, Long memberId);

    Optional<Participant> findByWorkspaceIdAndMemberId(Long workspaceId, Long memberId);

    Optional<Participant> findByIdAndWorkspaceIdAndDeletedAtIsNull(Long id, Long workspaceId);

    List<Participant> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    List<Participant> findAllByWorkspaceIdAndDeletedAtIsNull(Long workspaceId);

    long countByWorkspaceIdAndDeletedAtIsNull(Long workspaceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Participant p
            set p.deletedAt = null,
                p.permission = :permission,
                p.joinedAt = :joinedAt,
                p.createdBy = :createdBy
            where p.workspaceId = :workspaceId
              and p.memberId = :memberId
              and p.deletedAt is not null
            """)
    int restore(
            @Param("workspaceId") Long workspaceId,
            @Param("memberId") Long memberId,
            @Param("permission") Permission permission,
            @Param("joinedAt") OffsetDateTime joinedAt,
            @Param("createdBy") Long createdBy);
}
