package com.ubidict.backend.member.infra;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.domain.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);
}
