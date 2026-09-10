package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberErrorCode;
import org.springframework.stereotype.Component;

/**
 * 회원의 로그인 가능 여부를 검사한다.
 */
@Component
public class MemberStatusValidator {

    public void validateLoginable(Member member) {
        if (!member.getStatus().canLogin()) {
            throw new BusinessException(MemberErrorCode.MEMBER_LOGIN_NOT_ALLOWED);
        }
    }
}
