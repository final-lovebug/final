package com.ubidict.backend.member.service;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.implement.MemberReader;
import com.ubidict.backend.member.service.model.MemberSummary;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberDirectoryService implements MemberDirectory {

    private final MemberReader memberReader;

    @Override
    @Transactional(readOnly = true)
    public MemberSummary getSummary(Long memberId) {
        return toSummary(memberReader.read(memberId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, MemberSummary> getSummaries(Collection<Long> memberIds) {
        return memberReader.readAll(memberIds).stream().collect(Collectors.toMap(Member::getId, this::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long memberId) {
        return memberReader.exists(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberRole getRole(Long memberId) {
        return memberReader.read(memberId).getRole();
    }

    private MemberSummary toSummary(Member member) {
        return new MemberSummary(member.getId(), member.getDisplayName(), member.getEmail(), member.getStatus());
    }
}
