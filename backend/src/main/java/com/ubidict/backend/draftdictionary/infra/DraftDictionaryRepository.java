package com.ubidict.backend.draftdictionary.infra;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftDictionaryRepository extends JpaRepository<DraftDictionary, Long> {

    Optional<DraftDictionary> findByIdAndDeletedAtIsNull(Long id);

    Optional<DraftDictionary> findByDictionaryIdAndDeletedAtIsNull(Long dictionaryId);
}
