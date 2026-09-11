package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import org.springframework.stereotype.Component;

@Component
public class ReviewRequestRemover {

    public void remove(ReviewRequest reviewRequest, Long actorId) {
        reviewRequest.cancel(actorId);
    }
}
