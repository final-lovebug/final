package com.ubidict.backend.draftdocument.infra;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 삭제되지 않은 문서 초안만 읽도록 조회 메서드에 소프트 삭제 조건을 명시한다. */
public interface DraftDocumentRepository extends JpaRepository<DraftDocument, Long> {

    boolean existsByDocumentIdInAndStatusNotAndDeletedAtIsNull(
            Collection<Long> documentIds, DraftDocumentStatus status);

    Optional<DraftDocument> findByIdAndDeletedAtIsNull(Long id);

    List<DraftDocument> findAllByDocumentIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long documentId);

    Page<DraftDocument> findAllByDeletedAtIsNull(Pageable pageable);

    Page<DraftDocument> findAllByDocumentIdAndDeletedAtIsNull(Long documentId, Pageable pageable);

    Page<DraftDocument> findAllByStatusAndDeletedAtIsNull(DraftDocumentStatus status, Pageable pageable);

    Page<DraftDocument> findAllByDocumentIdAndStatusAndDeletedAtIsNull(
            Long documentId, DraftDocumentStatus status, Pageable pageable);
}
