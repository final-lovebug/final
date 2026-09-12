package com.ubidict.backend.workspace.domain;

import com.ubidict.backend.common.domain.BaseEntity;
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

/**
 * 워크스페이스에 소속되어 권한을 가진 회원.
 *
 * <p>워크스페이스를 @ManyToOne으로 참조하지 않고 식별자로만 가리킨다. 애그리게잇 경계를 식별자로 넘어 지연 로딩 프록시가 상위 레이어로 새는 경로를 막는다. DB에는 FK가
 * 그대로 있다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseEntity {

    public static final int MAX_PARTICIPANTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(nullable = false, updatable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Permission permission;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Participant(Long workspaceId, Long memberId, Permission permission, Long createdBy) {
        this.workspaceId = workspaceId;
        this.memberId = memberId;
        this.permission = permission;
        this.joinedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.createdBy = createdBy;
    }

    /**
     * 워크스페이스를 만든 회원을 소유자로 등록한다. 워크스페이스당 소유자는 정확히 1명이다.
     */
    public static Participant owner(Long workspaceId, Long memberId) {
        return new Participant(workspaceId, memberId, Permission.OWNER, memberId);
    }

    public static Participant join(Long workspaceId, Long memberId, Permission permission, Long invitedBy) {
        return new Participant(workspaceId, memberId, permission, invitedBy);
    }

    public void changePermission(Permission permission) {
        this.permission = permission;
    }

    public void demoteToAdmin() {
        this.permission = Permission.ADMIN;
    }

    public void promoteToOwner() {
        this.permission = Permission.OWNER;
    }

    public boolean isOwner() {
        return permission == Permission.OWNER;
    }

    public boolean canBeManagedBy(Permission actorPermission) {
        return !permission.isAtLeast(actorPermission);
    }
}
