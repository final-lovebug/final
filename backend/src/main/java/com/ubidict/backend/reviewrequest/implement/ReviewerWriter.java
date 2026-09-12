package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewerWriter {
    private final ReviewerRepository repository;

    public Reviewer write(Reviewer reviewer) {
        return repository.save(reviewer);
    }

    public void delete(Reviewer reviewer) {
        repository.delete(reviewer);
    }
}
