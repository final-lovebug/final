package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 결과가 거절됐을 때 작업이 어떻게 끝나는지.
 *
 * <p>초안 생성은 롤백돼야 하지만 <b>작업은 실패로 남아야 한다</b> — 그대로 {@code RUNNING}에 두면 문서 단위 중복 방지 정책 때문에 그 문서의 대조가
 * 영구히 막힌다. 반대로 인증·조회 실패는 작업을 건드리면 안 된다.
 */
class DraftDocumentCheckCallbackServiceTest {

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";

    private final DraftDocumentCheckExecutionService executionService = mock(DraftDocumentCheckExecutionService.class);
    private final DraftDocumentCheckCallbackService service = new DraftDocumentCheckCallbackService(executionService);

    @DisplayName("제안어가 본문과 맞지 않아 거절되면 작업을 실패로 끝내고 오류를 그대로 돌려준다.")
    @Test
    void complete_invalidResult() {
        willThrow(new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_RESULT))
                .given(executionService)
                .complete(anyLong(), anyString(), anyInt(), anyList());

        assertThatThrownBy(() -> service.complete(40L, REQUEST_ID, 3, List.of()))
                .isInstanceOf(BusinessException.class);

        verify(executionService).expire(40L, "문서 대조 결과가 올바르지 않습니다.");
    }

    @DisplayName("상관 식별자가 맞지 않는 콜백은 작업 상태를 건드리지 않는다.")
    @Test
    void complete_forbidden() {
        willThrow(new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN))
                .given(executionService)
                .complete(anyLong(), anyString(), anyInt(), anyList());

        assertThatThrownBy(() -> service.complete(40L, REQUEST_ID, 3, List.of()))
                .isInstanceOf(BusinessException.class);

        verify(executionService, never()).expire(anyLong(), any());
    }

    @DisplayName("없는 작업에 대한 콜백도 아무 작업을 실패시키지 않는다.")
    @Test
    void complete_notFound() {
        willThrow(new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_NOT_FOUND))
                .given(executionService)
                .complete(anyLong(), anyString(), anyInt(), anyList());

        assertThatThrownBy(() -> service.complete(40L, REQUEST_ID, 3, List.of()))
                .isInstanceOf(BusinessException.class);

        verify(executionService, never()).expire(anyLong(), any());
    }

    @DisplayName("실패 콜백은 사유를 그대로 실행 서비스에 넘긴다.")
    @Test
    void fail() {
        service.fail(40L, REQUEST_ID, "모델 호출이 시간 안에 끝나지 않았습니다.", "LLM_TIMEOUT");

        verify(executionService).fail(40L, REQUEST_ID, "모델 호출이 시간 안에 끝나지 않았습니다.");
    }
}
