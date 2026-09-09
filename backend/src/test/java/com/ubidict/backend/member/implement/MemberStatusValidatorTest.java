package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.domain.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberStatusValidatorTest {

    private final MemberStatusValidator memberStatusValidator = new MemberStatusValidator();

    @DisplayName("ACTIVE 상태의 회원은 로그인할 수 있다.")
    @Test
    void validateLoginable() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");

        // when & then
        assertThatCode(() -> memberStatusValidator.validateLoginable(member)).doesNotThrowAnyException();
    }

    @DisplayName("탈퇴한 회원은 로그인할 수 없다.")
    @Test
    void validateLoginable_withdrawn() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        member.withdraw();

        // when & then
        assertThatThrownBy(() -> memberStatusValidator.validateLoginable(member))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_LOGIN_NOT_ALLOWED));
    }
}
