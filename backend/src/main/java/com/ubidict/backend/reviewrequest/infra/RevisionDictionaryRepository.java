package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionDictionaryRepository extends JpaRepository<RevisionDictionary, Long> {
    List<RevisionDictionary> findByReviewRequestId(Long id);

    Optional<RevisionDictionary> findByReviewRequestIdAndReexamineRound(Long id, int round);
}
