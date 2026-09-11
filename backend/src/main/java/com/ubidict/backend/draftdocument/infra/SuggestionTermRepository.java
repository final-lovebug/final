package com.ubidict.backend.draftdocument.infra;

import com.ubidict.backend.draftdocument.domain.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionTermRepository extends JpaRepository<SuggestionTerm, Long> {
    Optional<SuggestionTerm> findByIdAndDeletedAtIsNull(Long id);

    Page<SuggestionTerm> findAllByDraftDocumentIdAndDeletedAtIsNull(Long id, Pageable pageable);

    Page<SuggestionTerm> findAllByDraftDocumentIdAndStatusAndDeletedAtIsNull(
            Long id, SuggestionTermStatus status, Pageable pageable);
}
