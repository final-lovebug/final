package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.Reexamine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReexamineRepository extends JpaRepository<Reexamine, Long> {

    List<Reexamine> findByReviewRequestIdOrderByRoundAsc(Long reviewRequestId);
}
