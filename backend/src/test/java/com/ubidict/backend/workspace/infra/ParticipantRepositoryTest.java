package com.ubidict.backend.workspace.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class ParticipantRepositoryTest extends RepositoryTestSupport {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("참여자를 저장하면 권한이 문자열로 저장된다.")
    @Test
    void save_permissionIsString() {
        // given
        Long workspaceId = saveWorkspace();

        // when
        Participant saved = participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .build());
        em.flush();
        em.clear();

        // then
        Participant found = participantRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPermission()).isEqualTo(Permission.OWNER);
        assertThat(found.getJoinedAt()).isNotNull();
    }

    @DisplayName("워크스페이스와 회원으로 참여자를 조회한다.")
    @Test
    void findByWorkspaceIdAndMemberIdAndDeletedAtIsNull() {
        // given
        Long workspaceId = saveWorkspace();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .build());
        em.flush();
        em.clear();

        // when
        Optional<Participant> found =
                participantRepository.findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, MEMBER_ID);

        // then
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getPermission()).isEqualTo(Permission.OWNER);
    }

    @DisplayName("소프트 삭제된 참여자는 조회되지 않는다.")
    @Test
    void findByWorkspaceIdAndMemberIdAndDeletedAtIsNull_participantIsDeleted() {
        // given
        Long workspaceId = saveWorkspace();
        Participant participant = participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .build());
        participant.delete();
        em.flush();
        em.clear();

        // when
        Optional<Participant> found =
                participantRepository.findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, MEMBER_ID);

        // then
        assertThat(found).isEmpty();
    }

    @DisplayName("회원이 참여한 워크스페이스의 참여자 행만 조회된다.")
    @Test
    void findAllByMemberIdAndDeletedAtIsNull() {
        // given
        Long mine = saveWorkspace();
        Long others = saveWorkspace();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(mine)
                .memberId(MEMBER_ID)
                .build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(others)
                .memberId(2L)
                .build());
        em.flush();
        em.clear();

        // when
        List<Participant> found = participantRepository.findAllByMemberIdAndDeletedAtIsNull(MEMBER_ID);

        // then
        assertThat(found).extracting(Participant::getWorkspaceId).containsExactly(mine);
    }

    @DisplayName("같은 워크스페이스에 같은 회원을 두 번 등록할 수 없다.")
    @Test
    void save_memberIsAlreadyParticipant() {
        // given
        Long workspaceId = saveWorkspace();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .build());
        em.flush();

        // when & then
        Participant duplicated = ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .build();

        assertThatThrownBy(() -> participantRepository.save(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("삭제하지 않은 참여자만 인원 수에 포함된다.")
    @Test
    void countByWorkspaceIdAndDeletedAtIsNull() {
        // given
        Long workspaceId = saveWorkspace();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .build());
        Participant left = participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(2L)
                .build());
        left.delete();
        em.flush();
        em.clear();

        // when
        long count = participantRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId);

        // then
        assertThat(count).isEqualTo(1);
    }

    private Long saveWorkspace() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        em.flush();

        return workspace.getId();
    }
}
