package com.ubidict.backend.workspace.infra.adapter;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.infra.MemberRepository;
import com.ubidict.backend.workspace.infra.port.MemberQueryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberQueryAdapter implements MemberQueryPort {

    private final MemberRepository memberRepository;

    @Override
    public Optional<Long> findActiveMemberIdByEmail(String email) {
        return memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE).map(Member::getId);
    }
}
