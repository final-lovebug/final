package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Reexamine;
import com.ubidict.backend.reviewrequest.infra.ReexamineRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReexamineReader {

    private final ReexamineRepository reexamineRepository;

    public List<Reexamine> readAll(Long reviewRequestId) {
        return reexamineRepository.findByReviewRequestIdOrderByRoundAsc(reviewRequestId);
    }
}
