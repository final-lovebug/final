package com.ubidict.backend.workspace.fixture;

import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.domain.Permission;
import java.time.OffsetDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public class InvitationFixture {

    public static InvitationBuilder invitation() {
        return new InvitationBuilder();
    }

    public static class InvitationBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private String inviteeEmail = "invitee@example.com";
        private String token = "fixture-token";
        private Permission permission = Permission.REGULAR;
        private InvitationStatus status = InvitationStatus.PENDING;
        private OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(1);
        private Long createdBy = 1L;

        public InvitationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InvitationBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public InvitationBuilder inviteeEmail(String inviteeEmail) {
            this.inviteeEmail = inviteeEmail;
            return this;
        }

        public InvitationBuilder token(String token) {
            this.token = token;
            return this;
        }

        public InvitationBuilder permission(Permission permission) {
            this.permission = permission;
            return this;
        }

        public InvitationBuilder status(InvitationStatus status) {
            this.status = status;
            return this;
        }

        public InvitationBuilder expiresAt(OffsetDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public InvitationBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Invitation build() {
            Invitation invitation =
                    Invitation.issue(workspaceId, inviteeEmail, token, permission, expiresAt, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(invitation, "id", id);
            }
            if (status != InvitationStatus.PENDING) {
                ReflectionTestUtils.setField(invitation, "status", status);
            }
            return invitation;
        }
    }
}
