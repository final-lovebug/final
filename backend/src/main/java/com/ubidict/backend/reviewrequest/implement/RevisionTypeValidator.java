package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import org.springframework.stereotype.Component;

@Component
public class RevisionTypeValidator {
    public void validate(ReviewRequest request, boolean document) {
        request.validateRevisionType(document);
    }
}
