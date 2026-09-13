package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Reexamine;
import com.ubidict.backend.reviewrequest.infra.ReexamineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReexamineWriter {

    private final ReexamineRepository reexamineRepository;

    public Reexamine write(Reexamine reexamine) {
        return reexamineRepository.save(reexamine);
    }
}
