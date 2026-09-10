package com.ubidict.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.implement.MemberReader;
import com.ubidict.backend.member.service.model.MemberSummary;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberDirectoryServiceTest {

    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberDirectoryService memberDirectoryService = new MemberDirectoryService(memberReader);

    @DisplayName("id로 조회하면 회원 요약 정보를 반환한다.")
    @Test
    void getSummary() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberReader.read(1L)).willReturn(member);

        // when
        MemberSummary summary = memberDirectoryService.getSummary(1L);

        // then
        assertThat(summary.displayName()).isEqualTo("member1");
        assertThat(summary.email()).isEqualTo("member@example.com");
    }

    @DisplayName("여러 id로 조회하면 회원 요약 정보 맵을 반환한다.")
    @Test
    void getSummaries() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberReader.readAll(List.of(1L))).willReturn(List.of(member));

        // when
        var summaries = memberDirectoryService.getSummaries(List.of(1L));

        // then
        assertThat(summaries).containsKey(member.getId());
    }

    @DisplayName("존재하는 회원 id면 exists가 true를 반환한다.")
    @Test
    void exists() {
        // given
        given(memberReader.exists(1L)).willReturn(true);

        // when & then
        assertThat(memberDirectoryService.exists(1L)).isTrue();
    }

    @DisplayName("회원의 역할을 조회할 수 있다.")
    @Test
    void getRole() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberReader.read(1L)).willReturn(member);

        // when & then
        assertThat(memberDirectoryService.getRole(1L)).isEqualTo(MemberRole.REGULAR);
    }
}
