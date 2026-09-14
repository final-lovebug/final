package com.ubidict.backend.revisionlog.infra.adapter;

import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.revisionlog.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.revisionlog.infra.port.DictionaryVersionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 조회 어댑터는 소비 도메인에 두고 사전집 도메인의 Repository만 참조한다(D-33). */
@Component("dictionaryTermQueryAdapterForRevisionLog")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "real")
public class DictionaryTermQueryAdapter implements DictionaryTermQueryPort {

    private final DictionaryRepository dictionaryRepository;
    private final TermRepository termRepository;

    @Override
    public Optional<Long> findDictionaryIdByVersion(Long workspaceId, int versionNo) {
        return dictionaryRepository
                .findByWorkspaceIdAndVersionVersionNo(workspaceId, versionNo)
                .map(dictionary -> dictionary.getId());
    }

    @Override
    public List<TermSnapshot> readTerms(Long dictionaryId) {
        return termRepository.findAllByDictionaryIdOrderByPreferredFormAsc(dictionaryId).stream()
                .map(term -> new TermSnapshot(term.getPreferredForm(), term.getEnglishName(), term.getDefinition()))
                .toList();
    }

    @Override
    public Optional<DictionaryVersionSnapshot> readVersion(Long workspaceId, int versionNo) {
        return dictionaryRepository
                .findByWorkspaceIdAndVersionVersionNo(workspaceId, versionNo)
                .map(dictionary -> new DictionaryVersionSnapshot(
                        dictionary.getId(), dictionary.getCreatedBy(), dictionary.publishedAt()));
    }
}
