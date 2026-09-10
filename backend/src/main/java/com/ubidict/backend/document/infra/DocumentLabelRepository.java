package com.ubidict.backend.document.infra;

import com.ubidict.backend.document.domain.DocumentLabel;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentLabelRepository extends JpaRepository<DocumentLabel, Long> {

    List<DocumentLabel> findAllByDocumentId(Long documentId);

    List<DocumentLabel> findAllByDocumentIdIn(Collection<Long> documentIds);
}
