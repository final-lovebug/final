package com.ubidict.backend.member.service;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.implement.MemberReader;
import com.ubidict.backend.member.implement.MemberWithdrawer;
import com.ubidict.backend.member.implement.MemberWriter;
import com.ubidict.backend.member.service.model.CreateMemberCommand;
import com.ubidict.backend.member.service.model.MemberResult;
import com.ubidict.backend.member.service.model.UpdateMemberCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final MemberWithdrawer memberWithdrawer;

    @Transactional
    public MemberResult create(CreateMemberCommand command) {
        Member member =
                memberWriter.create(command.email(), command.displayName(), command.provider(), command.providerId());
        return MemberResult.from(member);
    }

    @Transactional(readOnly = true)
    public MemberResult getById(Long memberId) {
        return MemberResult.from(memberReader.read(memberId));
    }

    @Transactional
    public MemberResult update(UpdateMemberCommand command) {
        Member member = memberWriter.update(command.memberId(), command.displayName());
        return MemberResult.from(member);
    }

    @Transactional
    public void withdraw(Long memberId) {
        memberWithdrawer.withdraw(memberId);
    }
}
