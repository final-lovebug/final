package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.domain.*;
import com.ubidict.backend.draftdocument.infra.SuggestionTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuggestionTermWriter {
    private final SuggestionTermRepository repo;

    public SuggestionTerm add(Long d, TextRange a, String o, String s, Long by) {
        return repo.save(SuggestionTerm.create(d, a, o, s, by));
    }

    public SuggestionTerm update(SuggestionTerm t, TextRange a, String o, String s) {
        t.edit(a, o, s);
        return t;
    }
}
