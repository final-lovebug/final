package com.ubidict.backend.draftdictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
}
