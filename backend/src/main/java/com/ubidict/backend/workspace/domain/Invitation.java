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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
// 「같은 워크스페이스·같은 대상에 대기 상태 초대는 1개」는 부분 유니크가 필요해 MySQL 8.4 에서 DB 로 지킬 수 없다.
// 애플리케이션이 검증하고, DB 는 그 조회를 받쳐 주는 인덱스만 갖는다.
@Table(indexes = @Index(name = "idx_invitation_workspace_status", columnList = "workspace_id, status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(length = 320)
    private String inviteeEmail;

    // 수락 요청이 워크스페이스를 모른 채 토큰만 갖고 오므로 전역 유일이다.
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
