package com.ubidict.backend.document.infra;

import com.ubidict.backend.document.domain.Document;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 소프트 삭제 조건과 워크스페이스 조건을 메서드 이름에 박아 조회에서 빠뜨릴 수 없게 한다.
 *
 * <p>단건 조회에도 워크스페이스 조건을 건다. 다른 워크스페이스 문서의 식별자를 넣어도 찾지 못해야 데이터 격리(NFR-WS-001)가 성립한다.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndDeletedAtIsNull(Long id);

    Optional<Document> findByIdAndWorkspaceIdAndDeletedAtIsNull(Long id, Long workspaceId);

    List<Document> findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long workspaceId);

    @Query("""
            select d from Document d
            where d.workspaceId = :workspaceId
              and d.deletedAt is null
              and exists (
                select 1 from DocumentLabel dl, Label l
                where dl.documentId = d.id and dl.labelId = l.id
                  and l.workspaceId = :workspaceId and l.name = :labelName
              )
            order by d.createdAt desc
            """)
    List<Document> findAllByWorkspaceIdAndLabelName(
            @Param("workspaceId") Long workspaceId, @Param("labelName") String labelName);
}
