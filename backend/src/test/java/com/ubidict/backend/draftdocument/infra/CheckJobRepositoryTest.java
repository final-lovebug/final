package com.ubidict.backend.draftdocument.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
}
