package com.ubidict.backend.draftdictionary.infra;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, Long> {

    Optional<ExtractionJob> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByWorkspaceIdAndStatusInAndDeletedAtIsNull(
            Long workspaceId, Collection<ExtractionJobStatus> statuses);
}
