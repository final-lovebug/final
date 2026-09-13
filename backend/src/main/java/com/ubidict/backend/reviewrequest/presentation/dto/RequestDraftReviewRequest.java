package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.service.model.RequestDictionaryReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.RequestDocumentReviewCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 문서 초안과 사전 초안이 같은 요청 본문을 쓴다 — 대상 초안은 경로가 가리키고 나머지는 리뷰 요청의 속성이다. */
public record RequestDraftReviewRequest(
        @NotBlank @Size(max = ReviewRequest.TITLE_MAX_LENGTH)
        String title,

        String description,
        List<Long> reviewerMemberIds) {

    public RequestDocumentReviewCommand toDocumentCommand(Long draftDocumentId, Long requesterId) {
        return new RequestDocumentReviewCommand(draftDocumentId, title, description, reviewers(), requesterId);
    }

    public RequestDictionaryReviewCommand toDictionaryCommand(Long draftDictionaryId, Long requesterId) {
        return new RequestDictionaryReviewCommand(draftDictionaryId, title, description, reviewers(), requesterId);
    }

    private List<Long> reviewers() {
        return reviewerMemberIds == null ? List.of() : List.copyOf(reviewerMemberIds);
    }
}
