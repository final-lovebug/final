package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.ErrorCode;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 워커의 콜백을 받는 진입점.
 *
 * <p><b>{@link DraftDocumentCheckExecutionService}와 나눈 이유</b> — 결과가 검증에서 거절되면 초안 생성 트랜잭션은 롤백돼야 하지만
 * <b>작업은 실패로 남아야 한다.</b> 한 트랜잭션 안에서는 둘을 동시에 만족할 수 없어서, 여기서 두 번째 트랜잭션으로 실패를 기록한다. 작업이
 * {@code RUNNING}에 남으면 문서 단위 중복 방지 정책 때문에 다음 요청이 계속 막힌다.
 *
 * <p>다만 <b>인증·조회 실패는 작업을 건드리지 않는다</b> — 그랬다가는 상관 식별자를 모르는 누군가가 남의 작업을 실패시킬 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentCheckCallbackService {

    /** 작업 상태를 바꾸지 않고 그대로 돌려보낼 실패들. */
    private static final Set<ErrorCode> NOT_JOB_FAULT = Set.of(
            DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN,
            DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_NOT_FOUND);

    private final DraftDocumentCheckExecutionService executionService;

    public void complete(Long checkJobId, String requestId, int documentVersionNo, List<CheckSuggestion> suggestions) {
        try {
            executionService.complete(checkJobId, requestId, documentVersionNo, suggestions);
        } catch (BusinessException exception) {
            if (NOT_JOB_FAULT.contains(exception.errorCode())) {
                throw exception;
            }
            log.warn(
                    "[DraftDocumentCheckCallbackService.complete] Check result rejected. checkJobId={}, code={}",
                    checkJobId,
                    exception.errorCode());
            executionService.expire(checkJobId, exception.errorCode().message());
            throw exception;
        }
    }

    public void fail(Long checkJobId, String requestId, String reason, String code) {
        log.warn(
                "[DraftDocumentCheckCallbackService.fail] Worker reported failure. checkJobId={}, code={}",
                checkJobId,
                code);
        executionService.fail(checkJobId, requestId, reason);
    }
}
