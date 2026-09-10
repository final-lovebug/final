package com.ubidict.backend.dictionary.presentation.dto;

import com.ubidict.backend.dictionary.service.model.ReviseDictionaryCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 새 버전에 담을 용어 전체를 싣는다. 서버는 이전 버전에서 복사하지 않으므로, 변경분만 보내면 나머지 용어가 사라진다.
 */
public record ReviseDictionaryRequest(@NotEmpty @Valid List<TermRequest> terms) {

    public ReviseDictionaryCommand toCommand(Long workspaceId, Long memberId) {
        return new ReviseDictionaryCommand(
                workspaceId,
                memberId,
                terms.stream().map(TermRequest::toCommand).toList());
    }
}
