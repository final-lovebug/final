package com.ubidict.backend.draftdocument.infra;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckJobRepository extends JpaRepository<CheckJob, Long> {

    Optional<CheckJob> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByDocumentIdAndStatusInAndDeletedAtIsNull(Long documentId, Collection<CheckJobStatus> statuses);
}
