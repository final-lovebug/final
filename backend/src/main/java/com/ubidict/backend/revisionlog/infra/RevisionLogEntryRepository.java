package com.ubidict.backend.revisionlog.infra;

import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionLogEntryRepository extends JpaRepository<RevisionLogEntry, Long> {

    List<RevisionLogEntry> findAllByRevisionLogId(Long revisionLogId);
}
