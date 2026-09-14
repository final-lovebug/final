package com.ubidict.backend.workspace.infra.port;

import java.util.Optional;

/** 이메일 초대 대상이 가입 회원이면 회원 식별자를 조회한다. */
public interface MemberQueryPort {

    Optional<Long> findActiveMemberIdByEmail(String email);
}
