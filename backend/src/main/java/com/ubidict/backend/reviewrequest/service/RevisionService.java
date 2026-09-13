package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.service.model.RevisionResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개정안 이력 조회만 담당한다.
 *
 * <p>최초 개정안 제출은 리뷰 요청 생성과 같은 트랜잭션이어야 하므로 DraftReviewRequestService가, 재교정 회차는 ReexamineService가 각자 만든다
 * (D-44). 그래서 이 service에는 쓰기 경로가 없다.
 */
@Service
@RequiredArgsConstructor
public class RevisionService {
    private final RevisionDocumentReader documents;
    private final RevisionDictionaryReader dictionaries;

    @Transactional(readOnly = true)
    public List<RevisionResult> documents(Long requestId, Integer round) {
        return documents.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RevisionResult> dictionaries(Long requestId, Integer round) {
        return dictionaries.read(requestId, round).stream()
                .map(RevisionResult::from)
                .toList();
    }
}
