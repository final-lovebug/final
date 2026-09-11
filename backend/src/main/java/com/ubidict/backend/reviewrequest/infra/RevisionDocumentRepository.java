package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionDocumentRepository extends JpaRepository<RevisionDocument, Long> {
    List<RevisionDocument> findByReviewRequestId(Long id);

    Optional<RevisionDocument> findByReviewRequestIdAndReexamineRound(Long id, int round);
}
