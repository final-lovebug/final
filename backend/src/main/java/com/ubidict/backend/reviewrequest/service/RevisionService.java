package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.service.model.RevisionResult;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개정안 이력 조회만 담당한다.
 *
 * <p>최초 개정안 제출은 리뷰 요청 생성과 같은 트랜잭션이어야 하므로 DraftReviewRequestService가, 재교정 회차는 ReexamineService가 각자 만든다
 * (D-44). 그래서 이 service에는 쓰기 경로가 없다.
 *
 * <p>조회에도 참여자 검증을 건다 — 인증이 없던 동안 이 두 엔드포인트만 요청자를 받지 않아 인가가 비어 있었다(T-INT-3). 비참여자에게는 403이 아니라
 * 404가 나가야 하므로 WorkspaceAccessValidator를 그대로 쓴다.
 */
@Service
@RequiredArgsConstructor
public class RevisionService {
    private final RevisionDocumentReader documents;
    private final RevisionDictionaryReader dictionaries;
    private final ReviewRequestReader reviewRequestReader;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional(readOnly = true)
    public List<RevisionResult> documents(Long requestId, Integer round, Long memberId) {
        validateAccessible(requestId, memberId);

        return documents.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RevisionResult> dictionaries(Long requestId, Integer round, Long memberId) {
        validateAccessible(requestId, memberId);

        return dictionaries.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }

    private void validateAccessible(Long reviewRequestId, Long memberId) {
        ReviewRequest reviewRequest = reviewRequestReader.read(reviewRequestId);
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);
    }
}
