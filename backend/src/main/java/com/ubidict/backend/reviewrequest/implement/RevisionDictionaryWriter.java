package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.infra.RevisionDictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionDictionaryWriter {
    private final RevisionDictionaryRepository repository;

    public RevisionDictionary write(RevisionDictionary revision) {
        return repository.save(revision);
    }
}
