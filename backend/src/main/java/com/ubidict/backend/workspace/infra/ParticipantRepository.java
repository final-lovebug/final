package com.ubidict.backend.workspace.infra;

import com.ubidict.backend.workspace.domain.Participant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(Long workspaceId, Long memberId);

    Optional<Participant> findByIdAndWorkspaceIdAndDeletedAtIsNull(Long id, Long workspaceId);

    List<Participant> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    List<Participant> findAllByWorkspaceIdAndDeletedAtIsNull(Long workspaceId);

    long countByWorkspaceIdAndDeletedAtIsNull(Long workspaceId);
}
