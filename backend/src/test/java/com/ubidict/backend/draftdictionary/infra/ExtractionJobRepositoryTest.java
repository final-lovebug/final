package com.ubidict.backend.draftdictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class ExtractionJobRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ExtractionJobRepository extractionJobRepository;

    @DisplayName("대기 중인 용어 추출 작업과 대상 문서를 저장하고 조회한다.")
    @Test
    void save() {
        ExtractionJob saved = extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L, 20L), 2L));
        em.flush();
        em.clear();

        assertThat(extractionJobRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .get()
                .satisfies(job -> {
                    assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.PENDING);
                    assertThat(job.getSourceDocumentIds()).containsExactlyInAnyOrder(10L, 20L);
                    assertThat(job.getRequestId()).isNull();
                });
    }

    @DisplayName("워커에게 넘긴 작업의 상관 식별자가 저장된다.")
    @Test
    void save_requestId() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.markDispatching("0d5c6f6e-0000-4000-8000-000000000001");
        ExtractionJob saved = extractionJobRepository.save(job);
        em.flush();
        em.clear();

        assertThat(extractionJobRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .get()
                .satisfies(found -> {
                    assertThat(found.getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
                    assertThat(found.matchesRequestId("0d5c6f6e-0000-4000-8000-000000000001"))
                            .isTrue();
                });
    }

    @DisplayName("제한 시간이 지나도록 끝나지 않은 작업만 회수 대상으로 찾는다.")
    @Test
    void findStale() {
        ExtractionJob stale = extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L), 2L));
        ExtractionJob done = ExtractionJob.create(2L, null, List.of(20L), 2L);
        done.markDispatching("0d5c6f6e-0000-4000-8000-000000000002");
        done.fail("이미 끝난 작업");
        extractionJobRepository.save(done);
        em.flush();
        em.clear();

        List<ExtractionJob> found = extractionJobRepository.findAllByStatusInAndUpdatedAtBeforeAndDeletedAtIsNull(
                List.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING),
                OffsetDateTime.now().plusMinutes(1));

        assertThat(found).extracting(ExtractionJob::getId).containsExactly(stale.getId());
    }

    @DisplayName("워크스페이스에 진행 중인 용어 추출 작업이 있는지 확인한다.")
    @Test
    void existsInProgress() {
        extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L), 2L));

        assertThat(extractionJobRepository.existsByWorkspaceIdAndStatusInAndDeletedAtIsNull(
                        1L, List.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING)))
                .isTrue();
    }

    /**
     * 「워크스페이스당 진행 중 작업은 1개」를 DB가 보장하는지 확인한다. in_progress_flag 생성 컬럼이 걸려 있어야 통과한다.
     *
     * <p>사전 검사(ExtractionJobCreationPolicyValidator)는 락 없는 스냅샷 읽기라 동시 요청 둘을 모두 통과시킨다. 그렇게 작업이
     * 둘 생기면 워커가 둘 다 돌아 <b>LLM 을 두 번 호출한다</b> — 이 제약이 그 마지막 방어선이다.
     */
    @DisplayName("한 워크스페이스에 진행 중인 추출 작업이 둘이면 저장할 수 없다.")
    @Test
    void save_inProgressJobIsDuplicated() {
        extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L), 2L));
        em.flush();

        assertThatThrownBy(() -> {
                    extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(20L), 2L));
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("발행된(RUNNING) 작업이 있어도 같은 워크스페이스에 새 작업을 만들 수 없다.")
    @Test
    void save_runningJobBlocksNewJob() {
        ExtractionJob running = ExtractionJob.create(1L, null, List.of(10L), 2L);
        running.markDispatching("0d5c6f6e-0000-4000-8000-000000000003");
        extractionJobRepository.save(running);
        em.flush();

        assertThatThrownBy(() -> {
                    extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(20L), 2L));
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 끝난 작업은 플래그가 NULL 이라 유니크 판정에서 빠진다. 그렇지 않으면 한 번 추출한 워크스페이스가 영영 다시 추출할 수 없다.
     */
    @DisplayName("끝난 작업은 몇 개든 쌓이고 새 작업을 막지 않는다.")
    @Test
    void save_terminalJobsDoNotBlock() {
        extractionJobRepository.save(failedJob(1L, "0d5c6f6e-0000-4000-8000-000000000004"));
        extractionJobRepository.save(failedJob(1L, "0d5c6f6e-0000-4000-8000-000000000005"));
        em.flush();

        ExtractionJob fresh = extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(30L), 2L));
        em.flush();
        em.clear();

        assertThat(extractionJobRepository.findByIdAndDeletedAtIsNull(fresh.getId()))
                .isPresent();
    }

    @DisplayName("워크스페이스가 다르면 진행 중인 작업이 동시에 있을 수 있다.")
    @Test
    void save_inProgressJobsAcrossWorkspaces() {
        extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L), 2L));
        extractionJobRepository.save(ExtractionJob.create(2L, null, List.of(20L), 2L));
        em.flush();
        em.clear();

        assertThat(extractionJobRepository.existsByWorkspaceIdAndStatusInAndDeletedAtIsNull(
                        1L, List.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING)))
                .isTrue();
        assertThat(extractionJobRepository.existsByWorkspaceIdAndStatusInAndDeletedAtIsNull(
                        2L, List.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING)))
                .isTrue();
    }

    private ExtractionJob failedJob(Long workspaceId, String requestId) {
        ExtractionJob job = ExtractionJob.create(workspaceId, null, List.of(10L), 2L);
        job.markDispatching(requestId);
        job.fail("이미 끝난 작업");
        return job;
    }
}
