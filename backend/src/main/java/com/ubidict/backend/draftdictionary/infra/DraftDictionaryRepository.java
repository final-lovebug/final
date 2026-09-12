package com.ubidict.backend.draftdictionary.infra;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DraftDictionaryRepository extends JpaRepository<DraftDictionary, Long> {

    boolean existsByWorkspaceIdAndStatusNotAndDeletedAtIsNull(Long workspaceId, DraftDictionaryStatus status);

    /**
     * 해당 문서를 유래 문서로 삼는 진행 중 초안이 있는지 본다.
     *
     * <p>{@code sourceDocumentIds}가 {@code @ElementCollection}이라 파생 쿼리로 표현할 수 없어 join을 쓴다.
     */
    @Query("""
            select count(draft) > 0
            from DraftDictionary draft
            join draft.sourceDocumentIds sourceDocumentId
            where draft.deletedAt is null
              and draft.status <> :status
              and sourceDocumentId = :documentId
            """)
    boolean existsOngoingBySourceDocumentId(
            @Param("documentId") Long documentId, @Param("status") DraftDictionaryStatus status);

    Optional<DraftDictionary> findByIdAndDeletedAtIsNull(Long id);

    Optional<DraftDictionary> findByDictionaryIdAndDeletedAtIsNull(Long dictionaryId);
}
