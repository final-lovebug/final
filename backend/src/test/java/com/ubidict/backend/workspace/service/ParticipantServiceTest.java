package com.ubidict.backend.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.domain.event.ParticipantRemovedEvent;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@RecordApplicationEvents
class ParticipantServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    @DisplayName("소유자가 아니면 참여자 권한을 변경할 수 없다.")
    @Test
    void changePermission_actorIsNotOwner() {
        Long workspaceId = createWorkspace();
        Participant actor = join(workspaceId, 2L, Permission.ADMIN);
        Participant target = join(workspaceId, 3L, Permission.REGULAR);

        assertThatThrownBy(() -> participantService.changePermission(new ChangePermissionCommand(
                        workspaceId, target.getId(), Permission.ADMIN, actor.getMemberId())))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED);
    }

    @DisplayName("소유권을 넘기면 기존 소유자가 관리자로 내려간다.")
    @Test
    void transferOwnership_demotesPreviousOwner() {
        Long workspaceId = createWorkspace();
        Participant target = join(workspaceId, 2L, Permission.ADMIN);

        participantService.transferOwnership(new TransferOwnershipCommand(workspaceId, target.getId(), OWNER_ID));

        assertThat(findByMemberId(workspaceId, OWNER_ID).getPermission()).isEqualTo(Permission.ADMIN);
    }

    @DisplayName("소유권을 넘긴 뒤에도 소유자는 정확히 한 명이다.")
    @Test
    void transferOwnership_keepsExactlyOneOwner() {
        Long workspaceId = createWorkspace();
        Participant target = join(workspaceId, 2L, Permission.ADMIN);

        participantService.transferOwnership(new TransferOwnershipCommand(workspaceId, target.getId(), OWNER_ID));

        assertThat(participantRepository.findAllByWorkspaceIdAndDeletedAtIsNull(workspaceId).stream()
                        .filter(participant -> participant.getPermission() == Permission.OWNER))
                .hasSize(1);
    }

    @DisplayName("소유자는 내보낼 수 없다.")
    @Test
    void remove_ownerIsNotRemovable() {
        Long workspaceId = createWorkspace();
        Participant owner = findByMemberId(workspaceId, OWNER_ID);

        assertThatThrownBy(() ->
                        participantService.remove(new RemoveParticipantCommand(workspaceId, owner.getId(), OWNER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_OWNER_CANNOT_BE_REMOVED);
    }

    @DisplayName("참여자를 내보내면 회원 식별자를 담은 이탈 이벤트를 발행한다.")
    @Test
    void remove_publishesEvent() {
        // given
        Long workspaceId = createWorkspace();
        Participant target = join(workspaceId, 2L, Permission.REGULAR);

        // when
        participantService.remove(new RemoveParticipantCommand(workspaceId, target.getId(), OWNER_ID));

        // then
        assertThat(applicationEvents.stream(ParticipantRemovedEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.workspaceId()).isEqualTo(workspaceId);
                    assertThat(event.memberId()).isEqualTo(target.getMemberId());
                    assertThat(event.occurredAt()).isNotNull();
                });
    }

    @DisplayName("참여자가 아니면 참여자 목록을 조회할 수 없다.")
    @Test
    void readAll_memberIsNotParticipant() {
        Long workspaceId = createWorkspace();

        assertNotFound(() -> participantService.readAll(workspaceId, 9L));
    }

    @DisplayName("참여자가 아니면 권한 변경 대상을 조회하지 않는다.")
    @Test
    void changePermission_memberIsNotParticipant() {
        Long workspaceId = createWorkspace();
        Participant target = join(workspaceId, 2L, Permission.REGULAR);

        assertNotFound(() -> participantService.changePermission(
                new ChangePermissionCommand(workspaceId, target.getId(), Permission.ADMIN, 9L)));
    }

    @DisplayName("참여자가 아니면 소유권 이전 대상을 조회하지 않는다.")
    @Test
    void transferOwnership_memberIsNotParticipant() {
        Long workspaceId = createWorkspace();
        Participant target = join(workspaceId, 2L, Permission.ADMIN);

        assertNotFound(() ->
                participantService.transferOwnership(new TransferOwnershipCommand(workspaceId, target.getId(), 9L)));
    }

    @DisplayName("삭제된 워크스페이스의 참여자는 내보낼 수 없다.")
    @Test
    void remove_workspaceIsDeleted() {
        Long workspaceId = createWorkspace();
        Participant target = join(workspaceId, 2L, Permission.REGULAR);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.delete();
        workspaceRepository.save(workspace);

        assertNotFound(
                () -> participantService.remove(new RemoveParticipantCommand(workspaceId, target.getId(), OWNER_ID)));
    }

    private Long createWorkspace() {
        return workspaceService
                .create(new CreateWorkspaceCommand("팀", OWNER_ID))
                .workspaceId();
    }

    private Participant join(Long workspaceId, Long memberId, Permission permission) {
        return participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(memberId)
                .permission(permission)
                .build());
    }

    private Participant findByMemberId(Long workspaceId, Long memberId) {
        return participantRepository
                .findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, memberId)
                .orElseThrow();
    }

    private void assertNotFound(ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }
}
