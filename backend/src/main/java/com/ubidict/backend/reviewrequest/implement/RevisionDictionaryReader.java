package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.infra.RevisionDictionaryRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionDictionaryReader {
    private final RevisionDictionaryRepository repository;

    public List<RevisionDictionary> read(Long id) {
        return repository.findByReviewRequestId(id);
    }

    public List<RevisionDictionary> read(Long reviewRequestId, Integer round) {
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
    public boolean existsByDraft(Long draftDictionaryId) {
        return repository.existsByDraftDictionaryId(draftDictionaryId);
    }

    public boolean exists(Long id, int round) {
        return repository.findByReviewRequestIdAndReexamineRound(id, round).isPresent();
    }

    public int currentRound(Long reviewRequestId) {
        return repository.findByReviewRequestId(reviewRequestId).stream()
                .mapToInt(RevisionDictionary::getReexamineRound)
                .max()
                .orElse(0);
    }

    public RevisionDictionary readCurrent(Long reviewRequestId) {
        return repository
                .findTopByReviewRequestIdOrderByReexamineRoundDesc(reviewRequestId)
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode
                                .REVIEW_REQUEST_REVISION_NOT_FOUND));
    }

    /**
     * 목록/상세 응답에 대상 사전집 id를 실어 보내기 위한 배치 조회(T-INT-12). 회차별로 여러 행이
     * 있을 수 있어(재교정) 리뷰 요청 id별로 가장 최신 회차만 남긴다.
     */
    public Map<Long, RevisionDictionary> readLatestByReviewRequestIds(Collection<Long> reviewRequestIds) {
        if (reviewRequestIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RevisionDictionary> latest = new HashMap<>();
        for (RevisionDictionary revision : repository.findAllByReviewRequestIdIn(reviewRequestIds)) {
            latest.merge(revision.getReviewRequestId(), revision, RevisionDictionaryReader::latestRound);
        }
        return latest;
    }

    private static RevisionDictionary latestRound(RevisionDictionary a, RevisionDictionary b) {
        return a.getReexamineRound() >= b.getReexamineRound() ? a : b;
    }
}
