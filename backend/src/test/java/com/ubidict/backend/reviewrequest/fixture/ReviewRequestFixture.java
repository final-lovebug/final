package com.ubidict.backend.reviewrequest.fixture;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import org.springframework.test.util.ReflectionTestUtils;

public class ReviewRequestFixture {

    public static ReviewRequestBuilder reviewRequest() {
        return new ReviewRequestBuilder();
    }

    public static class ReviewRequestBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private ReviewRequestType type = ReviewRequestType.DOCUMENT;
        private String title = "결제 문서 리뷰";
        private String description = "결제 문서의 개정안을 검토합니다.";
        private Long requesterId = 1L;
        private ReviewRequestStatus status = ReviewRequestStatus.PENDING_REVIEW;

        public ReviewRequestBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ReviewRequestBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public ReviewRequestBuilder type(ReviewRequestType type) {
            this.type = type;
            return this;
        }

        public ReviewRequestBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ReviewRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ReviewRequestBuilder requesterId(Long requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public ReviewRequestBuilder status(ReviewRequestStatus status) {
            this.status = status;
            return this;
        }

        public ReviewRequest build() {
            ReviewRequest reviewRequest =
                    ReviewRequest.create(workspaceId, type, title, description, requesterId, requesterId);
            if (id != null) {
                ReflectionTestUtils.setField(reviewRequest, "id", id);
            }
            if (status != ReviewRequestStatus.PENDING_REVIEW) {
                ReflectionTestUtils.setField(reviewRequest, "status", status);
            }

            return reviewRequest;
        }
    }
}
