package com.ubidict.backend.document.infra;

import com.ubidict.backend.document.domain.DocumentVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 버전 번호와 확정일시는 임베디드 값 객체 안에 있어 파생 쿼리 이름이 VersionVersionNo가 된다(version 안의 versionNo).
 */
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    Optional<DocumentVersion> findByDocumentIdAndVersionVersionNo(Long documentId, int versionNo);

    @Query("""
            select new com.ubidict.backend.document.infra.DocumentVersionSummary(
                v.documentId, v.version.versionNo, v.dictionaryVersionNo, v.version.publishedAt, v.createdBy)
            from DocumentVersion v
            where v.documentId = :documentId
            order by v.version.versionNo desc
            """)
    List<DocumentVersionSummary> findSummariesByDocumentId(@Param("documentId") Long documentId);

    /**
     * 문서마다 최신 확정 버전 하나씩을 본문 없이 읽는다. 목록 조회에서 문서 수만큼 질의하지 않기 위한 것이다.
     */
    @Query("""
            select new com.ubidict.backend.document.infra.DocumentVersionSummary(
                v.documentId, v.version.versionNo, v.dictionaryVersionNo, v.version.publishedAt, v.createdBy)
            from DocumentVersion v, Document d
            where d.id = v.documentId
              and v.version.versionNo = d.currentVersionNo
              and v.documentId in :documentIds
            """)
    List<DocumentVersionSummary> findCurrentSummaries(@Param("documentIds") Collection<Long> documentIds);
}
