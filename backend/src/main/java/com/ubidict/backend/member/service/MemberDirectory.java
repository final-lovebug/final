package com.ubidict.backend.member.service;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.service.model.MemberSummary;
import java.util.Collection;
import java.util.Map;

/**
 * 다른 도메인(Workspace, Dictionary, Document, ReviewRequest 등)이 Member를
 * 참조할 때 쓰는 공개 인터페이스다. 호출하는 쪽은 이 인터페이스만 보고,
 * {@code Member} 도메인 모델·Repository·JWT 내부는 알지 못한다
 * ({@code docs/ARCHITECTURE.md} 교차 도메인 참조 규칙).
 */
public interface MemberDirectory {

    MemberSummary getSummary(Long memberId);

    Map<Long, MemberSummary> getSummaries(Collection<Long> memberIds);

    boolean exists(Long memberId);

    MemberRole getRole(Long memberId);
}
