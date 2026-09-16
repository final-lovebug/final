package com.ubidict.backend.draftdocument.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class CheckJobRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private CheckJobRepository checkJobRepository;

    @DisplayName("대기 중인 문서 대조 작업을 저장하고 조회한다.")
    @Test
    void save() {
        CheckJob saved = checkJobRepository.save(CheckJob.create(10L, 20L));
        em.flush();
        em.clear();

        assertThat(checkJobRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .get()
                .extracting(CheckJob::getStatus, CheckJob::getDocumentId)
                .containsExactly(CheckJobStatus.PENDING, 10L);
    }

    @DisplayName("워커에게 넘긴 작업의 상관 식별자가 저장된다.")
    @Test
    void save_requestId() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        checkJob.markDispatching("0d5c6f6e-0000-4000-8000-000000000001");
        CheckJob saved = checkJobRepository.save(checkJob);
        em.flush();
        em.clear();

        assertThat(checkJobRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .get()
                .satisfies(found -> {
                    assertThat(found.getStatus()).isEqualTo(CheckJobStatus.RUNNING);
                    assertThat(found.matchesRequestId("0d5c6f6e-0000-4000-8000-000000000001"))
                            .isTrue();
                });
    }

    @DisplayName("제한 시간이 지나도록 끝나지 않은 작업만 회수 대상으로 찾는다.")
    @Test
    void findStale() {
        CheckJob stale = checkJobRepository.save(CheckJob.create(10L, 20L));
        CheckJob done = CheckJob.create(11L, 20L);
        done.markDispatching("0d5c6f6e-0000-4000-8000-000000000002");
        done.fail("이미 끝난 작업");
        checkJobRepository.save(done);
        em.flush();
        em.clear();

        List<CheckJob> found = checkJobRepository.findAllByStatusInAndUpdatedAtBeforeAndDeletedAtIsNull(
                List.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING),
                OffsetDateTime.now().plusMinutes(1));

        assertThat(found).extracting(CheckJob::getId).containsExactly(stale.getId());
    }

    @DisplayName("문서에 대기 또는 실행 중인 대조 작업이 있는지 확인한다.")
    @Test
    void existsInProgress() {
        checkJobRepository.save(CheckJob.create(10L, 20L));

        assertThat(checkJobRepository.existsByDocumentIdAndStatusInAndDeletedAtIsNull(
                        10L, List.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING)))
                .isTrue();
    }

    /**
     * 「문서당 진행 중 작업은 1개」를 DB가 보장하는지 확인한다. in_progress_flag 생성 컬럼이 걸려 있어야 통과한다.
     *
     * <p>사전 검사(CheckJobCreationPolicyValidator)는 락 없는 스냅샷 읽기라 동시 요청 둘을 모두 통과시킨다. 그렇게 작업이
     * 둘 생기면 워커가 둘 다 돌아 <b>LLM 을 두 번 호출한다</b> — 이 제약이 그 마지막 방어선이다.
     */
    @DisplayName("한 문서에 진행 중인 대조 작업이 둘이면 저장할 수 없다.")
    @Test
    void save_inProgressJobIsDuplicated() {
        checkJobRepository.save(CheckJob.create(10L, 20L));
        em.flush();

        assertThatThrownBy(() -> {
                    checkJobRepository.save(CheckJob.create(10L, 21L));
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("발행된(RUNNING) 작업이 있어도 같은 문서에 새 작업을 만들 수 없다.")
    @Test
    void save_runningJobBlocksNewJob() {
        CheckJob running = CheckJob.create(10L, 20L);
        running.markDispatching("0d5c6f6e-0000-4000-8000-000000000003");
        checkJobRepository.save(running);
        em.flush();

        assertThatThrownBy(() -> {
                    checkJobRepository.save(CheckJob.create(10L, 21L));
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 끝난 작업은 플래그가 NULL 이라 유니크 판정에서 빠진다. 그렇지 않으면 한 번 대조한 문서를 영영 다시 대조할 수 없다.
     */
    @DisplayName("끝난 작업은 몇 개든 쌓이고 새 작업을 막지 않는다.")
    @Test
    void save_terminalJobsDoNotBlock() {
        checkJobRepository.save(failedJob(10L, "0d5c6f6e-0000-4000-8000-000000000004"));
        checkJobRepository.save(failedJob(10L, "0d5c6f6e-0000-4000-8000-000000000005"));
        em.flush();

        CheckJob fresh = checkJobRepository.save(CheckJob.create(10L, 20L));
        em.flush();
        em.clear();

        assertThat(checkJobRepository.findByIdAndDeletedAtIsNull(fresh.getId())).isPresent();
    }

    @DisplayName("문서가 다르면 진행 중인 작업이 동시에 있을 수 있다.")
    @Test
    void save_inProgressJobsAcrossDocuments() {
        checkJobRepository.save(CheckJob.create(10L, 20L));
        checkJobRepository.save(CheckJob.create(11L, 20L));
        em.flush();
        em.clear();

        assertThat(checkJobRepository.existsByDocumentIdAndStatusInAndDeletedAtIsNull(
                        10L, List.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING)))
                .isTrue();
        assertThat(checkJobRepository.existsByDocumentIdAndStatusInAndDeletedAtIsNull(
                        11L, List.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING)))
                .isTrue();
    }

    private CheckJob failedJob(Long documentId, String requestId) {
        CheckJob job = CheckJob.create(documentId, 20L);
        job.markDispatching(requestId);
        job.fail("이미 끝난 작업");
        return job;
    }
}
