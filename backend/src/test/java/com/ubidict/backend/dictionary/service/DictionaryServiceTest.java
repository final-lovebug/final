package com.ubidict.backend.dictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.exception.TermErrorCode;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import com.ubidict.backend.dictionary.service.model.TermResult;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * 버전을 만드는 진입점은 발행 위임 포트가 호출하는 publish 하나다(DIC-7). ADMIN이 용어 목록을 직접 실어 보내던 임시 엔드포인트를 제거했으므로 이 테스트도 그 계약으로
 * 검증한다 — 기준 버전 불일치는 DictionaryVersionPublishAdapterTest가 본다.
 */
@RecordApplicationEvents
class DictionaryServiceTest extends IntegrationTestSupport {

    private static final Long ADMIN_ID = 1L;
    private static final Long REGULAR_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @DisplayName("사전집 버전을 반영하면 식별자와 버전만 담은 이벤트를 발행한다.")
    @Test
    void publish_publishesEvent() {
        Long workspaceId = createWorkspace();

        publishNext(workspaceId, "회원");

        DictionaryResult active = dictionaryService.readActive(workspaceId, ADMIN_ID, defaultQuery());
        assertThat(applicationEvents.stream(DictionaryRevisedEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.workspaceId()).isEqualTo(workspaceId);
                    assertThat(event.dictionaryId()).isEqualTo(active.dictionaryId());
                    assertThat(event.versionNo()).isEqualTo(active.versionNo());
                    assertThat(event.occurredAt()).isEqualTo(active.publishedAt());
                });
    }

    @DisplayName("표준어 접두어로 활성 사전집의 용어를 검색한다.")
    @Test
    void readActive_searchByKeyword() {
        Long workspaceId = createWorkspace();
        publishNext(workspaceId, "사전", "문서");

        DictionaryResult result = dictionaryService.readActive(
                workspaceId, ADMIN_ID, new DictionarySearchQuery(0, 20, "preferredForm,asc", "사전"));

        assertThat(result.terms().content())
                .extracting(TermResult::preferredForm)
                .containsExactly("사전");
    }

    @DisplayName("활성 사전집의 용어 목록을 페이지 단위로 조회한다.")
    @Test
    void readActive_termsArePaged() {
        Long workspaceId = createWorkspace();
        publishNext(workspaceId, "사전", "문서");

        DictionaryResult result = dictionaryService.readActive(
                workspaceId, ADMIN_ID, new DictionarySearchQuery(0, 1, "preferredForm,asc", null));

        assertThat(result.terms().content()).hasSize(1);
        assertThat(result.terms().totalElements()).isEqualTo(2);
    }

    @DisplayName("사전집이 없는 워크스페이스에 반영하면 버전 1이 만들어진다.")
    @Test
    void publish_firstVersion() {
        // given
        Long workspaceId = createWorkspace();

        // when
        int versionNo = dictionaryService.publish(workspaceId, 0, terms("회원", "문서"), ADMIN_ID);

        // then
        assertThat(versionNo).isEqualTo(1);
        DictionaryResult active = dictionaryService.readActive(workspaceId, ADMIN_ID, defaultQuery());
        assertThat(active.status()).isEqualTo(DictionaryStatus.ACTIVE);
        assertThat(active.publishedAt()).isNotNull();
        assertThat(active.terms().content())
                .extracting(TermResult::preferredForm)
                .containsExactly("문서", "회원");
    }

    /**
     * 활성 사전집이 순간에도 둘이 되면 유니크 제약에 걸린다. 보관 전환이 새 버전 INSERT보다 먼저 flush되는지까지 확인하는 케이스다.
     */
    @DisplayName("이미 사전집이 있으면 이전 버전이 보관되고 활성 사전집은 하나로 유지된다.")
    @Test
    void publish_nextVersion() {
        // given
        Long workspaceId = createWorkspace();
        dictionaryService.publish(workspaceId, 0, terms("회원"), ADMIN_ID);

        // when
        int versionNo = dictionaryService.publish(workspaceId, 1, terms("회원", "문서"), ADMIN_ID);

        // then
        assertThat(versionNo).isEqualTo(2);
        assertThat(dictionaryService.readVersions(workspaceId, ADMIN_ID, 0, 20).content())
                .filteredOn(version -> version.status() == DictionaryStatus.ACTIVE)
                .hasSize(1);
    }

    /**
     * 별도 스냅샷 테이블 없이 이력을 지키는 이 모델의 전제다.
     */
    @DisplayName("새 버전을 반영해도 이전 버전의 용어는 그대로 남는다.")
    @Test
    void publish_previousTermsRemain() {
        // given
        Long workspaceId = createWorkspace();
        publishNext(workspaceId, "회원", "문서");

        // when
        publishNext(workspaceId, "회원");

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
        publishNext(workspaceId, "회원");
        publishNext(workspaceId, "회원", "문서");

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
        publishNext(workspaceId, "회원");

        // when & then
        assertThatThrownBy(() -> dictionaryService.readVersion(workspaceId, 2, ADMIN_ID, defaultQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_NOT_FOUND);
    }

    @DisplayName("용어가 없으면 사전집 버전을 반영할 수 없다.")
    @Test
    void publish_termsAreEmpty() {
        // given
        Long workspaceId = createWorkspace();

        // when & then
        assertThatThrownBy(() -> dictionaryService.publish(workspaceId, 0, List.of(), ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_EMPTY_TERMS);
    }

    @DisplayName("같은 표준어가 둘이면 사전집 버전을 반영할 수 없다.")
    @Test
    void publish_preferredFormIsDuplicated() {
        // given
        Long workspaceId = createWorkspace();

        // when & then
        assertThatThrownBy(() -> dictionaryService.publish(workspaceId, 0, terms("회원", "회원"), ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_DUPLICATE_PREFERRED_FORM);
    }

    @DisplayName("REGULAR는 사전집 버전을 반영할 수 없다.")
    @Test
    void publish_permissionIsBelowAdmin() {
        // given
        Long workspaceId = createWorkspace();
        joinAs(workspaceId, REGULAR_ID, Permission.REGULAR);

        // when & then
        assertThatThrownBy(() -> dictionaryService.publish(workspaceId, 0, terms("회원"), REGULAR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    /**
     * 403을 주면 그 워크스페이스가 존재한다는 사실이 드러난다(NFR-WS-001).
     */
    @DisplayName("참여자가 아닌 사용자의 반영은 권한 부족이 아니라 조회 실패로 응답한다.")
    @Test
    void publish_memberIsNotParticipant() {
        // given
        Long workspaceId = createWorkspace();

        // when & then
        assertThatThrownBy(() -> dictionaryService.publish(workspaceId, 0, terms("회원"), OUTSIDER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("참여자가 아니면 사전집을 조회할 수 없다.")
    @Test
    void readActive_memberIsNotParticipant() {
        // given
        Long workspaceId = createWorkspace();
        publishNext(workspaceId, "회원");

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
        publishNext(workspaceId, "회원");
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.delete();
        workspaceRepository.save(workspace);

        // when & then
        assertThatThrownBy(() -> dictionaryService.readActive(workspaceId, ADMIN_ID, defaultQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    /** 발행은 기준 버전이 현재 활성 버전과 같아야 하므로, 현재 버전을 읽어 다음 버전으로 올린다. */
    private int publishNext(Long workspaceId, String... preferredForms) {
        int baseVersionNo = dictionaryService.readVersions(workspaceId, ADMIN_ID, 0, 1).content().stream()
                .mapToInt(DictionaryVersionResult::versionNo)
                .max()
                .orElse(0);

        return dictionaryService.publish(workspaceId, baseVersionNo, terms(preferredForms), ADMIN_ID);
    }

    private List<NewTerm> terms(String... preferredForms) {
        return Arrays.stream(preferredForms)
                .map(preferredForm -> new NewTerm(preferredForm, null, preferredForm + "에 대한 정의"))
                .toList();
    }

    private DictionarySearchQuery defaultQuery() {
        return new DictionarySearchQuery(0, 20, "preferredForm,asc", null);
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
