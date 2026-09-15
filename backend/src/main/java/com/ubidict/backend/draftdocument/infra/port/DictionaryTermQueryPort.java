package com.ubidict.backend.draftdocument.infra.port;

import java.util.List;
import java.util.OptionalInt;

public interface DictionaryTermQueryPort {
    List<TermSnapshot> readActiveTerms(Long workspaceId);

    /** 활성 사전집의 버전. 없으면 비어 있다 — 대조는 기준 사전집이 있어야 성립한다(D-93). */
    OptionalInt activeVersionNo(Long workspaceId);
}
