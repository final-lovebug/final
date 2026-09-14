package com.ubidict.backend.workspace.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.InvitationErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(length = 320)
    private String inviteeEmail;

    @Column(nullable = false, unique = true, length = 64, updatable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private OffsetDateTime acceptedAt;
    private Long acceptedParticipantId;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Invitation(
            Long workspaceId,
            String inviteeEmail,
            String token,
            Permission permission,
            OffsetDateTime expiresAt,
            Long createdBy) {
        this.workspaceId = workspaceId;
        this.inviteeEmail = inviteeEmail;
        this.token = token;
        this.permission = permission;
        this.status = InvitationStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }

    public static Invitation issue(
            Long workspaceId,
            String inviteeEmail,
            String token,
            Permission permission,
            OffsetDateTime expiresAt,
            Long createdBy) {
        if (permission == Permission.OWNER) {
            throw new BusinessException(InvitationErrorCode.INVITATION_OWNER_NOT_ALLOWED);
        }
        return new Invitation(workspaceId, inviteeEmail, token, permission, expiresAt, createdBy);
    }

    public void accept(Long participantId, OffsetDateTime acceptedAt) {
        if (!isAcceptable(acceptedAt)) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
        }
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = acceptedAt.truncatedTo(ChronoUnit.MICROS);
        this.acceptedParticipantId = participantId;
    }

    public void expire() {
        if (status == InvitationStatus.PENDING) {
            status = InvitationStatus.EXPIRED;
        }
    }

    public void cancel() {
        if (status != InvitationStatus.PENDING) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
        }
        status = InvitationStatus.CANCELED;
    }

    public boolean isAcceptable(OffsetDateTime now) {
        if (status != InvitationStatus.PENDING) {
            return false;
        }
        if (!now.isBefore(expiresAt)) {
            expire();
            return false;
        }
        return true;
    }
}
