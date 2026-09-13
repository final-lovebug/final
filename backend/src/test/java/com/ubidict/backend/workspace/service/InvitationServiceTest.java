package com.ubidict.backend.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.InvitationErrorCode;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.InvitationFixture;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.infra.InvitationRepository;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import com.ubidict.backend.workspace.service.model.CreateWorkspaceCommand;
import com.ubidict.backend.workspace.service.model.InvitationResult;
import com.ubidict.backend.workspace.service.model.IssueInvitationCommand;
import com.ubidict.backend.workspace.service.model.WorkspaceResult;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InvitationServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("정원 검사는 초대 발급이 아니라 수락 시점에 한다.")
    @Test
    void accept_participantLimitExceeded() {
        Long workspaceId = createWorkspace();
        for (long memberId = 2; memberId <= 5; memberId++) {
            join(workspaceId, memberId, Permission.REGULAR);
        }
        InvitationResult issued =
                invitationService.issue(new IssueInvitationCommand(workspaceId, null, Permission.REGULAR, OWNER_ID));

        assertThatThrownBy(() -> invitationService.accept(issued.token(), 6L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_PARTICIPANT_LIMIT_EXCEEDED);
    }

    @DisplayName("같은 대상에게 대기 중인 초대를 중복 발급할 수 없다.")
    @Test
    void issue_duplicatePending() {
        Long workspaceId = createWorkspace();
        IssueInvitationCommand command =
                new IssueInvitationCommand(workspaceId, "invitee@example.com", Permission.REGULAR, OWNER_ID);
        invitationService.issue(command);

        assertThatThrownBy(() -> invitationService.issue(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_DUPLICATE_PENDING);
    }

    @DisplayName("이미 참여 중인 회원에게 이메일 초대를 발급할 수 없다.")
    @Test
    void issue_alreadyParticipant() {
        memberRepository.save(Member.create("owner@example.com", "소유자", OAuthProvider.GOOGLE, "owner-google-id"));
        Long workspaceId = createWorkspace();
        Member member =
                memberRepository.save(Member.create("invitee@example.com", "초대 대상", OAuthProvider.GOOGLE, "google-id"));
        join(workspaceId, member.getId(), Permission.REGULAR);

        assertThatThrownBy(() -> invitationService.issue(
                        new IssueInvitationCommand(workspaceId, member.getEmail(), Permission.REGULAR, OWNER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_ALREADY_PARTICIPANT);
    }

    @DisplayName("초대를 수락하면 초대에 지정된 권한으로 참여자가 만들어진다.")
    @Test
    void accept_createsParticipantWithGrantedPermission() {
        Long workspaceId = createWorkspace();
        InvitationResult issued =
                invitationService.issue(new IssueInvitationCommand(workspaceId, null, Permission.ADMIN, OWNER_ID));

        WorkspaceResult result = invitationService.accept(issued.token(), 2L);

        Participant participant = participantRepository
                .findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, 2L)
                .orElseThrow();
        Invitation invitation = invitationRepository.findByToken(issued.token()).orElseThrow();
        assertThat(result.myPermission()).isEqualTo(Permission.ADMIN);
        assertThat(participant.getPermission()).isEqualTo(Permission.ADMIN);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedParticipantId()).isEqualTo(participant.getId());
    }

    @DisplayName("이미 참여 중인 회원은 초대를 수락할 수 없다.")
    @Test
    void accept_alreadyParticipant() {
        Long workspaceId = createWorkspace();
        join(workspaceId, 2L, Permission.REGULAR);
        Invitation invitation = saveInvitation(workspaceId, OWNER_ID);

        assertThatThrownBy(() -> invitationService.accept(invitation.getToken(), 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_ALREADY_PARTICIPANT);
    }

    @DisplayName("내보낸 회원이 초대를 다시 수락하면 기존 참여자 행을 되살린다.")
    @Test
    void accept_removedParticipantRestoresExistingRow() {
        Long workspaceId = createWorkspace();
        Participant removed = join(workspaceId, 2L, Permission.REGULAR);
        removed.delete();
        participantRepository.save(removed);
        Invitation invitation = saveInvitation(workspaceId, OWNER_ID);

        invitationService.accept(invitation.getToken(), 2L);

        Participant restored = participantRepository
                .findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, 2L)
                .orElseThrow();
        assertThat(restored.getId()).isEqualTo(removed.getId());
        assertThat(restored.getPermission()).isEqualTo(Permission.REGULAR);
        assertThat(restored.isDeleted()).isFalse();
    }

    @DisplayName("알 수 없는 토큰으로 초대를 수락하면 404 예외가 발생한다.")
    @Test
    void accept_tokenIsUnknown() {
        assertThatThrownBy(() -> invitationService.accept("unknown-token", 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_NOT_FOUND);
    }

    @DisplayName("참여자가 아니면 초대를 발급할 수 없다.")
    @Test
    void issue_memberIsNotParticipant() {
        Long workspaceId = createWorkspace();

        assertThatThrownBy(() ->
                        invitationService.issue(new IssueInvitationCommand(workspaceId, null, Permission.REGULAR, 2L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("일반 참여자는 초대 목록을 조회할 수 없다.")
    @Test
    void readAll_permissionIsBelowAdmin() {
        Long workspaceId = createWorkspace();
        join(workspaceId, 2L, Permission.REGULAR);

        assertThatThrownBy(() -> invitationService.readAll(workspaceId, 2L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    @DisplayName("삭제된 워크스페이스의 초대는 조회할 수 없다.")
    @Test
    void readAll_workspaceIsDeleted() {
        Long workspaceId = createWorkspace();
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.delete();
        workspaceRepository.save(workspace);

        assertThatThrownBy(() -> invitationService.readAll(workspaceId, OWNER_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("초대 목록을 조회하면 만료된 대기 초대를 만료 상태로 저장한다.")
    @Test
    void readAll_expiresPendingInvitation() {
        Long workspaceId = createWorkspace();
        Invitation invitation = invitationRepository.save(InvitationFixture.invitation()
                .workspaceId(workspaceId)
                .expiresAt(OffsetDateTime.now().minusSeconds(1))
                .build());

        assertThat(invitationService.readAll(workspaceId, OWNER_ID, InvitationStatus.EXPIRED))
                .extracting(InvitationResult::invitationId)
                .containsExactly(invitation.getId());
        assertThat(invitationRepository
                        .findById(invitation.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(InvitationStatus.EXPIRED);
    }

    @DisplayName("만료된 토큰으로 수락을 시도하면 만료 상태를 저장한다.")
    @Test
    void accept_expiredInvitationPersistsStatus() {
        Long workspaceId = createWorkspace();
        Invitation invitation = invitationRepository.save(InvitationFixture.invitation()
                .workspaceId(workspaceId)
                .expiresAt(OffsetDateTime.now().minusSeconds(1))
                .build());

        assertThatThrownBy(() -> invitationService.accept(invitation.getToken(), 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
        assertThat(invitationRepository
                        .findById(invitation.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(InvitationStatus.EXPIRED);
    }

    @DisplayName("초대 발급자는 현재 권한이 일반 참여자여도 자기 초대를 취소할 수 있다.")
    @Test
    void cancel_actorIsIssuer() {
        Long workspaceId = createWorkspace();
        join(workspaceId, 2L, Permission.REGULAR);
        Invitation invitation = saveInvitation(workspaceId, 2L);

        invitationService.cancel(workspaceId, invitation.getId(), 2L);

        assertThat(invitationRepository
                        .findById(invitation.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(InvitationStatus.CANCELED);
    }

    @DisplayName("발급자가 아닌 일반 참여자는 초대를 취소할 수 없다.")
    @Test
    void cancel_actorIsNotIssuerOrAdmin() {
        Long workspaceId = createWorkspace();
        join(workspaceId, 2L, Permission.REGULAR);
        Invitation invitation = saveInvitation(workspaceId, OWNER_ID);

        assertThatThrownBy(() -> invitationService.cancel(workspaceId, invitation.getId(), 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    private Long createWorkspace() {
        return workspaceService
                .create(new CreateWorkspaceCommand("개발팀", OWNER_ID))
                .workspaceId();
    }

    private Participant join(Long workspaceId, Long memberId, Permission permission) {
        return participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(memberId)
                .permission(permission)
                .build());
    }

    private Invitation saveInvitation(Long workspaceId, Long createdBy) {
        return invitationRepository.save(InvitationFixture.invitation()
                .workspaceId(workspaceId)
                .createdBy(createdBy)
                .build());
    }
}
