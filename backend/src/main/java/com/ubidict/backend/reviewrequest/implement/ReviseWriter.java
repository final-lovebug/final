package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Revise;
import com.ubidict.backend.reviewrequest.infra.ReviseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviseWriter {

    private final ReviseRepository reviseRepository;

    public Revise write(Revise revise) {
        return reviseRepository.save(revise);
    }
}
