package com.ubidict.backend.dictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.exception.TermErrorCode;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import com.ubidict.backend.dictionary.service.model.ReviseDictionaryCommand;
import com.ubidict.backend.dictionary.service.model.TermCommand;
import com.ubidict.backend.dictionary.service.model.TermResult;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DictionaryServiceTest extends IntegrationTestSupport {

    @Test
    @DisplayName("표준어 접두어로 활성 사전집의 용어를 검색한다.")
    void readActive_searchByKeyword() {
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("사전"), term("문서")));
        DictionaryResult result = dictionaryService.readActive(
                workspaceId, ADMIN_ID, new DictionarySearchQuery(0, 20, "preferredForm,asc", "사전"));
        assertThat(result.terms().content())
                .extracting(TermResult::preferredForm)
                .containsExactly("사전");
    }

    @Test
    @DisplayName("활성 사전집의 용어 목록을 페이지 단위로 조회한다.")
    void readActive_termsArePaged() {
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("사전"), term("문서")));
        DictionaryResult result = dictionaryService.readActive(
                workspaceId, ADMIN_ID, new DictionarySearchQuery(0, 1, "preferredForm,asc", null));
        assertThat(result.terms().content()).hasSize(1);
        assertThat(result.terms().totalElements()).isEqualTo(2);
    }

    private static final Long ADMIN_ID = 1L;
    private static final Long REGULAR_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @DisplayName("사전집이 없는 워크스페이스에 반영하면 버전 1이 만들어진다.")
    @Test
    void revise_firstVersion() {
        // given
        Long workspaceId = createWorkspace();

        // when
        DictionaryResult result = dictionaryService.revise(command(workspaceId, term("회원"), term("문서")));

        // then
        assertThat(result.versionNo()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(DictionaryStatus.ACTIVE);
        assertThat(result.publishedAt()).isNotNull();
        assertThat(result.terms().content())
                .extracting(TermResult::preferredForm)
                .containsExactly("문서", "회원");
    }

    /**
     * 활성 사전집이 순간에도 둘이 되면 유니크 제약에 걸린다. 보관 전환이 새 버전 INSERT보다 먼저 flush되는지까지 확인하는 케이스다.
     */
    @DisplayName("이미 사전집이 있으면 이전 버전이 보관되고 활성 사전집은 하나로 유지된다.")
    @Test
    void revise_nextVersion() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("회원")));

        // when
        DictionaryResult result = dictionaryService.revise(command(workspaceId, term("회원"), term("문서")));

        // then
        assertThat(result.versionNo()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(DictionaryStatus.ACTIVE);
        assertThat(dictionaryService.readVersions(workspaceId, ADMIN_ID, 0, 20).content())
                .filteredOn(version -> version.status() == DictionaryStatus.ACTIVE)
                .hasSize(1);
    }

    /**
     * 별도 스냅샷 테이블 없이 이력을 지키는 이 모델의 전제다.
     */
    @DisplayName("새 버전을 반영해도 이전 버전의 용어는 그대로 남는다.")
    @Test
    void revise_previousTermsRemain() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("회원"), term("문서")));

        // when
        dictionaryService.revise(command(workspaceId, term("회원")));

        // then
        DictionaryResult first = dictionaryService.readVersion(workspaceId, 1, ADMIN_ID, defaultQuery());
        assertThat(first.status()).isEqualTo(DictionaryStatus.ARCHIVED);
        assertThat(first.terms().content())
                .extracting(TermResult::preferredForm)
                .containsExactly("문서", "회원");
    }

    @DisplayName("버전 이력은 최신 버전이 먼저 나오고 용어 수를 함께 담는다.")
    @Test
    void readVersions() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("회원")));
        dictionaryService.revise(command(workspaceId, term("회원"), term("문서")));

        // when
        List<DictionaryVersionResult> versions =
                dictionaryService.readVersions(workspaceId, ADMIN_ID, 0, 20).content();

        // then
        assertThat(versions).extracting(DictionaryVersionResult::versionNo).containsExactly(2, 1);
        assertThat(versions).extracting(DictionaryVersionResult::termCount).containsExactly(2L, 1L);
    }

    @DisplayName("사전집을 만든 적 없는 워크스페이스의 버전 이력은 비어 있다.")
    @Test
    void readVersions_dictionaryDoesNotExist() {
        // given
        Long workspaceId = createWorkspace();

        // when & then
        assertThat(dictionaryService.readVersions(workspaceId, ADMIN_ID, 0, 20).content())
                .isEmpty();
    }

    @DisplayName("사전집이 없으면 현재 확정본을 조회할 수 없다.")
    @Test
    void readActive_dictionaryDoesNotExist() {
        // given
        Long workspaceId = createWorkspace();

        // when & then
        assertThatThrownBy(() -> dictionaryService.readActive(workspaceId, ADMIN_ID, defaultQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_NOT_FOUND);
    }

    @DisplayName("없는 버전 번호로는 조회할 수 없다.")
    @Test
    void readVersion_versionNoDoesNotExist() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("회원")));

        // when & then
        assertThatThrownBy(() -> dictionaryService.readVersion(workspaceId, 2, ADMIN_ID, defaultQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_NOT_FOUND);
    }

    @DisplayName("용어가 없으면 사전집 버전을 반영할 수 없다.")
    @Test
    void revise_termsAreEmpty() {
        // given
        Long workspaceId = createWorkspace();
        ReviseDictionaryCommand command = new ReviseDictionaryCommand(workspaceId, ADMIN_ID, List.of());

        // when & then
        assertThatThrownBy(() -> dictionaryService.revise(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_EMPTY_TERMS);
    }

    @DisplayName("같은 표준어가 둘이면 사전집 버전을 반영할 수 없다.")
    @Test
    void revise_preferredFormIsDuplicated() {
        // given
        Long workspaceId = createWorkspace();
        ReviseDictionaryCommand command = command(workspaceId, term("회원"), term("회원"));

        // when & then
        assertThatThrownBy(() -> dictionaryService.revise(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_DUPLICATE_PREFERRED_FORM);
    }

    @DisplayName("REGULAR는 사전집 버전을 반영할 수 없다.")
    @Test
    void revise_permissionIsBelowAdmin() {
        // given
        Long workspaceId = createWorkspace();
        joinAs(workspaceId, REGULAR_ID, Permission.REGULAR);
        ReviseDictionaryCommand command = new ReviseDictionaryCommand(workspaceId, REGULAR_ID, List.of(term("회원")));

        // when & then
        assertThatThrownBy(() -> dictionaryService.revise(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    /**
     * 403을 주면 그 워크스페이스가 존재한다는 사실이 드러난다(NFR-WS-001).
     */
    @DisplayName("참여자가 아닌 사용자의 반영은 권한 부족이 아니라 조회 실패로 응답한다.")
    @Test
    void revise_memberIsNotParticipant() {
        // given
        Long workspaceId = createWorkspace();
        ReviseDictionaryCommand command = new ReviseDictionaryCommand(workspaceId, OUTSIDER_ID, List.of(term("회원")));

        // when & then
        assertThatThrownBy(() -> dictionaryService.revise(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("참여자가 아니면 사전집을 조회할 수 없다.")
    @Test
    void readActive_memberIsNotParticipant() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("회원")));

        // when & then
        assertThatThrownBy(() -> dictionaryService.readActive(workspaceId, OUTSIDER_ID, defaultQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    /**
     * 워크스페이스를 소프트 삭제해도 참여자 행은 남는다. 사전집은 워크스페이스를 읽을 이유가 없으므로 검증기가 생존을 확인하지 않으면 그대로 새어 나간다.
     */
    @DisplayName("삭제된 워크스페이스의 사전집은 조회할 수 없다.")
    @Test
    void readActive_workspaceIsDeleted() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.revise(command(workspaceId, term("회원")));
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.delete();
        workspaceRepository.save(workspace);

        // when & then
        assertThatThrownBy(() -> dictionaryService.readActive(workspaceId, ADMIN_ID, defaultQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    private ReviseDictionaryCommand command(Long workspaceId, TermCommand... terms) {
        return new ReviseDictionaryCommand(workspaceId, ADMIN_ID, List.of(terms));
    }

    private DictionarySearchQuery defaultQuery() {
        return new DictionarySearchQuery(0, 20, "preferredForm,asc", null);
    }

    private TermCommand term(String preferredForm) {
        return new TermCommand(preferredForm, null, preferredForm + "에 대한 정의");
    }

    private Long createWorkspace() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        joinAs(workspace.getId(), ADMIN_ID, Permission.OWNER);

        return workspace.getId();
    }

    private void joinAs(Long workspaceId, Long memberId, Permission permission) {
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(memberId)
                .permission(permission)
                .build());
    }
}
