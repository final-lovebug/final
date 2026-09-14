package com.ubidict.backend.draftdocument.infra;

import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionTermRepository extends JpaRepository<SuggestionTerm, Long> {
    Optional<SuggestionTerm> findByIdAndDeletedAtIsNull(Long id);

    Page<SuggestionTerm> findAllByDraftDocumentIdAndDeletedAtIsNull(Long id, Pageable pageable);

    Page<SuggestionTerm> findAllByDraftDocumentIdAndStatusAndDeletedAtIsNull(
            Long id, SuggestionTermStatus status, Pageable pageable);

    List<SuggestionTerm> findAllByDraftDocumentIdAndDeletedAtIsNull(Long draftDocumentId);
}
