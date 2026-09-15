package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionDocumentReader {
    private final RevisionDocumentRepository repository;

    public List<RevisionDocument> read(Long id) {
        return repository.findByReviewRequestId(id);
    }

    public List<RevisionDocument> read(Long reviewRequestId, Integer round) {
        if (round == null) {
            return read(reviewRequestId);
        }
        return List.of(repository
                .findByReviewRequestIdAndReexamineRound(reviewRequestId, round)
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode
                                .REVIEW_REQUEST_REVISION_NOT_FOUND)));
    }

    /** 이 초안으로 만든 개정안이 이미 있는지 본다 — 초안 상태 전이가 AFTER_COMMIT 비동기라 그것만으로는 중복 요청을 막지 못한다. */
    public boolean existsByDraft(Long draftDocumentId) {
        return repository.existsByDraftDocumentId(draftDocumentId);
    }

    public boolean exists(Long id, int round) {
        return repository.findByReviewRequestIdAndReexamineRound(id, round).isPresent();
    }

    public int currentRound(Long reviewRequestId) {
        return repository.findByReviewRequestId(reviewRequestId).stream()
                .mapToInt(RevisionDocument::getReexamineRound)
                .max()
                .orElse(0);
    }

    public RevisionDocument readCurrent(Long reviewRequestId) {
        return repository
                .findTopByReviewRequestIdOrderByReexamineRoundDesc(reviewRequestId)
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode
                                .REVIEW_REQUEST_REVISION_NOT_FOUND));
    }

    /**
     * 목록/상세 응답에 대상 문서 id를 실어 보내기 위한 배치 조회(T-INT-12). 회차별로 여러 행이
     * 있을 수 있어(재교정) 리뷰 요청 id별로 가장 최신 회차만 남긴다.
     */
    public Map<Long, RevisionDocument> readLatestByReviewRequestIds(Collection<Long> reviewRequestIds) {
        if (reviewRequestIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RevisionDocument> latest = new HashMap<>();
        for (RevisionDocument revision : repository.findAllByReviewRequestIdIn(reviewRequestIds)) {
            latest.merge(revision.getReviewRequestId(), revision, RevisionDocumentReader::latestRound);
        }
        return latest;
    }

    private static RevisionDocument latestRound(RevisionDocument a, RevisionDocument b) {
        return a.getReexamineRound() >= b.getReexamineRound() ? a : b;
    }
}
