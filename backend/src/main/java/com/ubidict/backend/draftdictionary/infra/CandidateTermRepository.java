package com.ubidict.backend.draftdictionary.infra;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidateTermRepository extends JpaRepository<CandidateTerm, Long> {

    List<CandidateTerm> findAllByDraftDictionaryIdAndStatusInAndDeletedAtIsNullOrderByFormAsc(
            Long draftDictionaryId, Collection<CandidateTermStatus> statuses);

    Optional<CandidateTerm> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByDraftDictionaryIdAndFormAndDeletedAtIsNull(Long draftDictionaryId, String form);

    @Query("""
            select candidate
            from CandidateTerm candidate
            where candidate.draftDictionaryId = :draftDictionaryId
              and candidate.deletedAt is null
              and (:status is null or candidate.status = :status)
              and (:form is null or candidate.form like concat('%', :form, '%'))
              and (:minOccurrenceCount is null or candidate.occurrenceCount >= :minOccurrenceCount)
            """)
    Page<CandidateTerm> search(
            @Param("draftDictionaryId") Long draftDictionaryId,
            @Param("status") CandidateTermStatus status,
            @Param("form") String form,
            @Param("minOccurrenceCount") Integer minOccurrenceCount,
            Pageable pageable);
}
