package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.Revise;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviseRepository extends JpaRepository<Revise, Long> {

    Optional<Revise> findByReviewRequestId(Long reviewRequestId);
}
