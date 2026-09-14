package com.ubidict.backend.revisionlog.service;

import static com.ubidict.backend.revisionlog.fixture.RevisionLogFixture.dictionaryLog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import com.ubidict.backend.revisionlog.exception.RevisionLogErrorCode;
import com.ubidict.backend.revisionlog.infra.RevisionLogEntryRepository;
import com.ubidict.backend.revisionlog.infra.RevisionLogRepository;
import com.ubidict.backend.revisionlog.service.model.RevisionLogDetailResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogSearchQuery;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevisionLogServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;

    @Autowired
    private RevisionLogService revisionLogService;

    @Autowired
    private RevisionLogRepository revisionLogRepository;

    @Autowired
    private RevisionLogEntryRepository revisionLogEntryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private Long workspaceId;

    @BeforeEach
    void setUpWorkspace() {
        workspaceId =
                workspaceRepository.save(Workspace.create("개정 이력 팀", OWNER_ID)).getId();
        participantRepository.save(Participant.owner(workspaceId, OWNER_ID));
        participantRepository.save(Participant.join(workspaceId, MEMBER_ID, Permission.REGULAR, OWNER_ID));
    }

    @DisplayName("목록은 축과 선택한 대상을 기준으로 최신순 페이징한다.")
    @Test
    void search_filtersByTarget() {
        saveDictionaryLog(10L, 1);
        saveDictionaryLog(20L, 2);

        PageResult<RevisionLogResult> result = revisionLogService.search(
                RevisionLogSearchQuery.of(workspaceId, MEMBER_ID, RevisionLogTargetType.DICTIONARY, 10L, 0, 20, null));

        assertThat(result.content()).singleElement().satisfies(log -> {
            assertThat(log.targetId()).isEqualTo(10L);
            assertThat(log.versionNo()).isEqualTo(1);
        });
    }

    @DisplayName("상세는 같은 워크스페이스의 변경 항목만 함께 조회한다.")
    @Test
    void read_returnsEntries() {
        RevisionLog revisionLog = saveDictionaryLog(10L, 1);
        revisionLogEntryRepository.save(
                RevisionLogEntry.term(revisionLog.getId(), RevisionLogChangeType.ADDED, "사용자", "User", null));

        RevisionLogDetailResult result = revisionLogService.read(workspaceId, revisionLog.getId(), MEMBER_ID);

        assertThat(result.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.subject()).isEqualTo("사용자");
            assertThat(entry.subjectEnglishName()).isEqualTo("User");
        });
    }

    @DisplayName("비참여자는 개정 이력을 조회하면 워크스페이스를 찾지 못한다.")
    @Test
    void search_notParticipant() {
        assertThatThrownBy(() -> revisionLogService.search(RevisionLogSearchQuery.of(
                        workspaceId, OUTSIDER_ID, RevisionLogTargetType.DICTIONARY, null, 0, 20, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    @DisplayName("다른 워크스페이스의 개정 이력은 존재 여부를 드러내지 않는다.")
    @Test
    void read_differentWorkspace() {
        RevisionLog revisionLog = saveDictionaryLog(10L, 1);
        Long otherWorkspaceId =
                workspaceRepository.save(Workspace.create("다른 팀", OWNER_ID)).getId();
        participantRepository.save(Participant.owner(otherWorkspaceId, OWNER_ID));

        assertThatThrownBy(() -> revisionLogService.read(otherWorkspaceId, revisionLog.getId(), OWNER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(RevisionLogErrorCode.REVISION_LOG_NOT_FOUND));
    }

    private RevisionLog saveDictionaryLog(Long dictionaryId, int versionNo) {
        return revisionLogRepository.saveAndFlush(dictionaryLog()
                .workspaceId(workspaceId)
                .dictionaryId(dictionaryId)
                .versionNo(versionNo)
                .previousVersionNo(versionNo == 1 ? null : versionNo - 1)
                .build());
    }
}
