package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.fixture.TermFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermOrigin;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.infra.adapter.DictionaryTermQueryAdapter;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * G-1 통합 입력 — 초안 생성이 이전 사전집의 용어를 그대로 이어받는지 본다.
 */
class DraftDictionaryWriterTest extends RepositoryTestSupport {

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private CandidateTermRepository candidateTermRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private DraftDictionaryWriter writer;
    private Long workspaceId;

    @BeforeEach
    void setUp() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
        writer = new DraftDictionaryWriter(
                draftDictionaryRepository,
                candidateTermRepository,
                new DictionaryTermQueryAdapter(dictionaryRepository, termRepository));
    }

    @DisplayName("이전 사전집의 용어는 정의까지 승계한다.")
    @Test
    void create_inheritsDefinitionOfActiveTerms() {
        Long dictionaryId = saveActiveDictionary();
        termRepository.save(TermFixture.term()
                .dictionaryId(dictionaryId)
                .preferredForm("결제수단")
                .englishName("PaymentMethod")
                .definition("회원이 결제에 사용하는 수단")
                .build());
        em.flush();
        em.clear();

        DraftDictionary draft = writer.create(workspaceId, dictionaryId, List.of(10L), 7L);
        em.flush();
        em.clear();

        List<CandidateTerm> candidates =
                candidateTermRepository.findAllByDraftDictionaryIdAndStatusInAndDeletedAtIsNullOrderByFormAsc(
                        draft.getId(), List.of(CandidateTermStatus.KEPT));

        assertThat(candidates)
                .extracting(
                        CandidateTerm::getForm,
                        CandidateTerm::getProposedDefinition,
                        CandidateTerm::getProposedEnglishName,
                        CandidateTerm::getOrigin)
                .containsExactly(tuple("결제수단", "회원이 결제에 사용하는 수단", "PaymentMethod", CandidateTermOrigin.EXISTING));
    }

    @DisplayName("사전집이 없는 첫 회차는 승계할 용어가 없다.")
    @Test
    void create_firstRoundHasNoInheritedTerm() {
        DraftDictionary draft = writer.create(workspaceId, null, List.of(10L), 7L);
        em.flush();
        em.clear();

        assertThat(candidateTermRepository.findAllByDraftDictionaryIdAndStatusInAndDeletedAtIsNullOrderByFormAsc(
                        draft.getId(), List.of(CandidateTermStatus.KEPT)))
                .isEmpty();
    }

    private Long saveActiveDictionary() {
        Dictionary dictionary = dictionaryRepository.save(
                DictionaryFixture.dictionary().workspaceId(workspaceId).build());
        return dictionary.getId();
    }
}
