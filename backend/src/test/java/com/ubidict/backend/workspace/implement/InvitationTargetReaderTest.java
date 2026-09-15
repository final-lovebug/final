package com.ubidict.backend.workspace.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.infra.port.MemberQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationTargetReaderTest {

    @Mock
    private MemberQueryPort memberQueryPort;

    @Mock
    private ParticipantReader participantReader;

    @InjectMocks
    private InvitationTargetReader invitationTargetReader;

    @DisplayName("초대 이메일이 현재 참여 중인 회원의 이메일이면 참여 대상으로 판정한다.")
    @Test
    void isParticipant_activeParticipant() {
        given(memberQueryPort.findActiveMemberIdByEmail("invitee@example.com")).willReturn(Optional.of(2L));
        given(participantReader.readOptional(1L, 2L))
                .willReturn(Optional.of(
                        ParticipantFixture.participant().memberId(2L).build()));

        assertThat(invitationTargetReader.isParticipant(1L, "invitee@example.com"))
                .isTrue();
    }

    @DisplayName("링크 복사 초대는 특정 이메일 대상이 없으므로 참여자로 미리 판정하지 않는다.")
    @Test
    void isParticipant_inviteeEmailIsAbsent() {
        assertThat(invitationTargetReader.isParticipant(1L, null)).isFalse();
    }
}
