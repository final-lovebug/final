package com.ubidict.backend.draftdictionary.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.presentation.dto.AddCandidateTermRequest;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.member.infra.security.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CandidateTermController.class)
@AutoConfigureMockMvc(addFilters = false)
class CandidateTermControllerTest {
    @MockitoBean
    DraftDictionaryService draftDictionaryService;

    @MockitoBean
    com.ubidict.backend.draftdictionary.service.CandidateTermService candidateTermService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    void addValidationAndPageFormatContractExists() {
        var request = new AddCandidateTermRequest("term", "definition", null, List.of(1L), 1, List.of("context"));
        assertThat(request.form()).isNotBlank();
        assertThat(request.occurrenceCount()).isPositive();
        assertThat(request.occurredDocumentIds()).containsExactly(1L);
    }
}
