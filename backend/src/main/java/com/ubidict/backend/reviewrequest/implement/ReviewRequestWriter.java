package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRequestWriter {

    private final ReviewRequestRepository reviewRequestRepository;

    public ReviewRequest write(ReviewRequest reviewRequest) {
        return reviewRequestRepository.save(reviewRequest);
    }
}
