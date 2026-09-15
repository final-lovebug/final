package com.ubidict.backend.document.infra.port;

import java.util.Optional;

public interface DictionaryQueryPort {
    Optional<Integer> activeVersionNo(Long workspaceId);
}
