package com.ubidict.backend.workspace.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.fixture.InvitationFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class InvitationRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("초대 토큰은 전역에서 유일하다.")
    @Test
    void save_tokenIsUnique() {
        Long workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        invitationRepository.save(InvitationFixture.invitation()
                .workspaceId(workspaceId)
                .token("duplicate-token")
                .build());
        Invitation duplicate = InvitationFixture.invitation()
                .workspaceId(workspaceId)
                .token("duplicate-token")
                .build();

        assertThatThrownBy(() -> invitationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("토큰으로 초대를 조회한다.")
    @Test
    void findByToken() {
        Long workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        Invitation invitation = invitationRepository.save(
                InvitationFixture.invitation().workspaceId(workspaceId).build());

        assertThat(invitationRepository.findByToken(invitation.getToken())).contains(invitation);
    }

    @DisplayName("워크스페이스와 상태로 초대를 조회한다.")
    @Test
    void findAllByWorkspaceIdAndStatus() {
        Long workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        invitationRepository.save(
                InvitationFixture.invitation().workspaceId(workspaceId).build());

        assertThat(invitationRepository.findAllByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING))
                .hasSize(1);
    }
}
