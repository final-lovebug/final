package com.ubidict.backend.member.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 도메인 모델이 JPA 엔티티를 겸한다({@code docs/ARCHITECTURE.md} 참고).
 *
 * <p>{@code createdBy}는 두지 않는다. 회원은 스스로 가입하는 주체라 생성자를 지정할
 * 자연스러운 대상이 없다({@code docs/DOMAIN.md} 참고, Member 한정 예외).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "member",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
            @UniqueConstraint(
                    name = "uk_member_provider",
                    columnNames = {"provider", "provider_id"})
        })
public class Member extends BaseEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(nullable = false)
    private String providerId;

    private Member(String email, String displayName, OAuthProvider provider, String providerId) {
        this.email = email;
        this.displayName = displayName;
        this.provider = provider;
        this.providerId = providerId;
        this.status = MemberStatus.ACTIVE;
        this.role = MemberRole.REGULAR;
    }

    /**
     * 소셜 로그인을 전제로 하므로 생성 즉시 {@code ACTIVE}, {@code REGULAR}로 시작한다.
     */
    public static Member create(String email, String displayName, OAuthProvider provider, String providerId) {
        return new Member(email, displayName, provider, providerId);
    }

    public void changeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("표시 이름은 비어 있을 수 없습니다.");
        }
        this.displayName = displayName;
    }

    /**
     * 탈퇴 처리한다. 소셜 연동 해제(Unlink)는 로그인 도메인(별도 티켓)에서 처리하고,
     * 여기서는 상태 변경과 소프트 삭제만 한다.
     */
    public void withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            return;
        }
        status = MemberStatus.WITHDRAWN;
        delete();
    }
}
