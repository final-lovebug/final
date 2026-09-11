package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.*;
import com.ubidict.backend.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CandidateTermServiceTest extends IntegrationTestSupport {
    @Autowired
    CandidateTermService candidateTermService;

    @Test
    void crudAndSearchContractExists() {
        var candidate =
                CandidateTerm.create(1L, "term", "definition", "english", List.of(3L), 2, List.of("context"), 9L);
        candidate.edit(null, "updated", null);
        assertThat(candidate.getForm()).isEqualTo("term");
        assertThat(candidate.getProposedDefinition()).isEqualTo("updated");
        candidate.delete();
        assertThat(candidate.isDeleted()).isTrue();
    }
}
