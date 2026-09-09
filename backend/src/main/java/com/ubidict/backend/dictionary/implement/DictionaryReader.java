package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 활성 사전집이 곧 가장 최근 확정본이다. 워크스페이스에 사전집이 없는 기간은 정상이므로, 호출자가 그 경우를 다뤄야 하면 readActiveOptional을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class DictionaryReader {

    private final DictionaryRepository dictionaryRepository;

    public Dictionary readActive(Long workspaceId) {
        return readActiveOptional(workspaceId)
                .orElseThrow(() -> new BusinessException(DictionaryErrorCode.DICTIONARY_NOT_FOUND));
    }

    public Optional<Dictionary> readActiveOptional(Long workspaceId) {
        return dictionaryRepository.findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE);
    }

    public Dictionary readByVersionNo(Long workspaceId, int versionNo) {
        return dictionaryRepository
                .findByWorkspaceIdAndVersionVersionNo(workspaceId, versionNo)
                .orElseThrow(() -> new BusinessException(DictionaryErrorCode.DICTIONARY_NOT_FOUND));
    }

    /**
     * 최신 버전이 먼저 나온다. 사전집을 만든 적 없는 워크스페이스는 빈 목록이다.
     */
    public List<Dictionary> readAllVersions(Long workspaceId) {
        return dictionaryRepository.findAllByWorkspaceIdOrderByVersionVersionNoDesc(workspaceId);
    }
}
