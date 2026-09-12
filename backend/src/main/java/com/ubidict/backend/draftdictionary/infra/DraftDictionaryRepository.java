package com.ubidict.backend.draftdictionary.infra;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftDictionaryRepository extends JpaRepository<DraftDictionary, Long> {

    boolean existsByWorkspaceIdAndStatusNotAndDeletedAtIsNull(Long workspaceId, DraftDictionaryStatus status);

    Optional<DraftDictionary> findByIdAndDeletedAtIsNull(Long id);

    Optional<DraftDictionary> findByDictionaryIdAndDeletedAtIsNull(Long dictionaryId);
}
