package com.ubidict.backend.workspace.infra;

import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByIdAndWorkspaceId(Long invitationId, Long workspaceId);

    Optional<Invitation> findByToken(String token);

    List<Invitation> findAllByWorkspaceId(Long workspaceId);

    List<Invitation> findAllByWorkspaceIdAndStatus(Long workspaceId, InvitationStatus status);

    boolean existsByWorkspaceIdAndInviteeEmailAndStatus(Long workspaceId, String inviteeEmail, InvitationStatus status);
}
