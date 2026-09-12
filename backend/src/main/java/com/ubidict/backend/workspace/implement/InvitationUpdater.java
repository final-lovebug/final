package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Invitation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InvitationUpdater {

    public void refreshExpiration(List<Invitation> invitations, OffsetDateTime now) {
        invitations.forEach(invitation -> invitation.isAcceptable(now));
    }

    public void accept(Invitation invitation, Long participantId, OffsetDateTime acceptedAt) {
        invitation.accept(participantId, acceptedAt);
    }

    public void cancel(Invitation invitation) {
        invitation.cancel();
    }
}
