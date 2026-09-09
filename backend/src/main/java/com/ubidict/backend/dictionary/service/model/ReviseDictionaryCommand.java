package com.ubidict.backend.dictionary.service.model;

import com.ubidict.backend.dictionary.domain.NewTerm;
import java.util.List;

/**
 * 새 사전집 버전에 담을 내용. 이전 버전에서 복사하지 않으므로 그 버전의 용어 전체가 실려 있어야 한다.
 */
public record ReviseDictionaryCommand(Long workspaceId, Long memberId, List<TermCommand> terms) {

    public List<NewTerm> toNewTerms() {
        return terms.stream().map(TermCommand::toNewTerm).toList();
    }
}
