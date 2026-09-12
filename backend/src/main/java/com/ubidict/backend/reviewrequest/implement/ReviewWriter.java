package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.infra.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewWriter {

    private final ReviewRepository reviewRepository;

    public Review write(Review review) {
        return reviewRepository.save(review);
    }
}
