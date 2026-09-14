package com.ubidict.backend.member.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.member.infra.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    // 암호화 후 길이가 평문보다 길어져(AES-GCM nonce+tag+Base64) 컬럼을 넉넉히 잡는다
    // (개인정보 처리 방침 9/13, V3__widen_member_pii_columns.sql). 왜 암호화하는지는
    // MemberFieldEncryptor 참고.
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 500)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 500)
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
     * 탈퇴 처리한다. 소셜 연동 해제(Unlink)는 로컬 상태 변경(soft delete)까지만 의미한다 —
     * Google API를 호출해 토큰을 revoke하지 않는다({@code docs/DOMAIN.md} 인증·회원가입
     * 정책, 9/10 확정).
     *
     * <p>개인정보 파기 정책(9/13): email/displayName/providerId를 회원 고유 id 기반 값으로
     * 치환한다(익명화). email/providerId까지 바꾸는 이유 — {@code uk_member_email}/
     * {@code uk_member_provider} 유니크 제약을 유지하면서, 같은 Google 계정이 나중에 다시
     * 로그인하면(재가입) {@code MemberRegistrar.find}가 이 탈퇴한 row를 더 이상 찾지 못하게
     * 해서 새 회원으로 재가입할 수 있게 한다 — 탈퇴가 그 계정을 영구히 막지 않는다.
     */
    public void withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            return;
        }
        this.email = "withdrawn-%d@deleted.local".formatted(id);
        this.displayName = "탈퇴한 회원";
        this.providerId = "withdrawn-%d".formatted(id);
        status = MemberStatus.WITHDRAWN;
        delete();
    }
}
