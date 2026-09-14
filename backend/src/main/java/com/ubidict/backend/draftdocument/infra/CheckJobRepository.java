package com.ubidict.backend.draftdocument.infra;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckJobRepository extends JpaRepository<CheckJob, Long> {

    Optional<CheckJob> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByDocumentIdAndStatusInAndDeletedAtIsNull(Long documentId, Collection<CheckJobStatus> statuses);

    /**
     * 제한 시간이 지나도록 끝나지 않은 작업. 타임아웃 스위퍼가 쓴다(D-77).
     *
     * <p>{@code updatedAt} 기준이라 「아직 발행되지 않은 PENDING」과 「워커가 붙잡고 있는 RUNNING」을 같은 시계로 잰다.
     */
    List<CheckJob> findAllByStatusInAndUpdatedAtBeforeAndDeletedAtIsNull(
            Collection<CheckJobStatus> statuses, OffsetDateTime threshold);
}
