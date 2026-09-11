package com.ubidict.backend.workspace.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {
    /**
     * 참여자가 아닌 워크스페이스에도 이 코드를 쓴다. 403을 주면 워크스페이스의 존재가 드러나므로 존재 자체를 숨긴다.
     */
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_INVALID_NAME(HttpStatus.BAD_REQUEST, "워크스페이스 이름은 1자 이상 50자 이하여야 합니다."),
    WORKSPACE_INVALID_REVIEWER_COUNT(HttpStatus.BAD_REQUEST, "필수 리뷰어 수는 0 이상이어야 합니다."),
    WORKSPACE_REVIEWER_COUNT_EXCEEDS_PARTICIPANTS(HttpStatus.BAD_REQUEST, "필수 리뷰어 수는 참여자 수를 넘을 수 없습니다."),
    WORKSPACE_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "워크스페이스 관리자 이상만 수행할 수 있습니다."),
    WORKSPACE_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "워크스페이스 소유자만 수행할 수 있습니다."),
    WORKSPACE_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    WORKSPACE_PARTICIPANT_NOT_MANAGEABLE(HttpStatus.FORBIDDEN, "해당 참여자를 관리할 수 없습니다."),
    WORKSPACE_OWNER_NOT_REMOVABLE(HttpStatus.CONFLICT, "소유자는 내보낼 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
