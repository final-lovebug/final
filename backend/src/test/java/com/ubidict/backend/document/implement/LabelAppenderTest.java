package com.ubidict.backend.document.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.infra.LabelRepository;
import com.ubidict.backend.support.MySqlIntegrationTestSupport;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * D-94. 라벨 이름 비교가 DB의 collation(utf8mb4_0900_ai_ci)과 같은 기준으로 동작하는지 본다.
 *
 * <p>MySQL 위에서 돌려야 의미가 있는 검증이라 단위 테스트가 아니라 통합 테스트다 — 자바만 보면 통과하지만 DB가 막는 경우가 정확히 Y-36이었다.
 */
class LabelAppenderTest extends MySqlIntegrationTestSupport {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    private LabelAppender labelAppender;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long workspaceId;

    @BeforeEach
    void openWorkspace() {
        workspaceId = workspaceRepository
                .save(WorkspaceFixture.workspace().name("개발팀").build())
                .getId();
    }

    /**
     * Y-36의 재현 케이스. 고치기 전에는 여기서 uk_label_workspace_name 위반이 나고 세션이 rollback-only가 되어 500이었다.
     */
    @DisplayName("대소문자만 다른 이름은 기존 라벨을 재사용한다.")
    @Test
    void appendMissing_reusesLabelIgnoringCase() {
        // given
        appendMissing("api");

        // when
        List<Label> labels = appendMissing("API");

        // then
        assertThat(labels).hasSize(1);
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
    }

    @DisplayName("재사용한 라벨의 표시명은 최초 생성 시 입력값을 유지한다.")
    @Test
    void appendMissing_keepsFirstCreatedDisplayName() {
        // given
        appendMissing("api");

        // when
        List<Label> labels = appendMissing("API");

        // then
        assertThat(labels).extracting(Label::getName).containsExactly("api");
    }

    @DisplayName("한 요청에 대소문자만 다른 이름이 함께 오면 라벨은 하나만 만들어진다.")
    @Test
    void appendMissing_foldsDuplicatesWithinRequest() {
        // when
        List<Label> labels = appendMissing("API", "api");

        // then
        assertThat(labels).extracting(Label::getName).containsExactly("API");
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
    }

    @DisplayName("서로 다른 이름은 요청 순서대로 돌려준다.")
    @Test
    void appendMissing_keepsRequestedOrder() {
        // when
        List<Label> labels = appendMissing("설계", "결제", "정산");

        // then
        assertThat(labels).extracting(Label::getName).containsExactly("설계", "결제", "정산");
    }

    /**
     * D-94가 트랜잭션을 나눈 결과다. 문서 생성이 뒤에서 실패해도 라벨은 워크스페이스에 남는다 —
     * 「문서에서 라벨을 떼도 라벨 자체는 남는다」와 같은 취급이다.
     */
    @DisplayName("바깥 트랜잭션이 롤백돼도 만들어진 라벨은 남는다.")
    @Test
    void appendMissing_survivesOuterRollback() {
        // when
        assertThatRollsBack(() -> labelAppender.appendMissing(workspaceId, List.of("설계"), MEMBER_ID));

        // then
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .extracting(Label::getName)
                .containsExactly("설계");
    }

    private List<Label> appendMissing(String... names) {
        return transactionTemplate.execute(
                status -> labelAppender.appendMissing(workspaceId, List.of(names), MEMBER_ID));
    }

    private void assertThatRollsBack(Runnable work) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                work.run();
                throw new IllegalStateException("문서 생성이 실패한 상황을 흉내 낸다.");
            });
        } catch (IllegalStateException expected) {
            // 바깥 트랜잭션만 롤백된다.
        }
    }
}
