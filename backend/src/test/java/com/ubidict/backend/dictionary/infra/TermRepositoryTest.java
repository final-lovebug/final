package com.ubidict.backend.dictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.fixture.TermFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TermRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("용어를 저장하면 표준어·영문명·정의가 그대로 저장된다.")
    @Test
    void save() {
        // given
        Long dictionaryId = saveDictionary(saveWorkspace(), 1, DictionaryStatus.ACTIVE);

        // when
        Term saved = termRepository.save(
                TermFixture.term().dictionaryId(dictionaryId).build());
        em.flush();
        em.clear();

        // then
        Term found = termRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPreferredForm()).isEqualTo("회원");
        assertThat(found.getEnglishName()).isEqualTo("Member");
        assertThat(found.getDefinition()).isEqualTo("서비스에 가입해 인증받는 주체");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @DisplayName("사전집의 용어는 표준어 오름차순으로 조회된다.")
    @Test
    void findAllByDictionaryIdOrderByPreferredFormAsc() {
        // given
        Long dictionaryId = saveDictionary(saveWorkspace(), 1, DictionaryStatus.ACTIVE);
        saveTerm(dictionaryId, "워크스페이스");
        saveTerm(dictionaryId, "사전집");
        saveTerm(dictionaryId, "문서");
        em.flush();
        em.clear();

        // when
        List<Term> terms = termRepository.findAllByDictionaryIdOrderByPreferredFormAsc(dictionaryId);

        // then
        assertThat(terms).extracting(Term::getPreferredForm).containsExactly("문서", "사전집", "워크스페이스");
    }

    @DisplayName("여러 사전집의 용어를 한 번에 조회한다.")
    @Test
    void findAllByDictionaryIdIn() {
        // given
        Long workspaceId = saveWorkspace();
        Long archived = saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        Long active = saveDictionary(workspaceId, 2, DictionaryStatus.ACTIVE);
        saveTerm(archived, "회원");
        saveTerm(active, "회원");
        saveTerm(active, "문서");
        em.flush();
        em.clear();

        // when
        List<Term> terms = termRepository.findAllByDictionaryIdIn(List.of(archived, active));

        // then
        assertThat(terms).hasSize(3);
        assertThat(terms).extracting(Term::getDictionaryId).containsOnly(archived, active);
    }

    @DisplayName("한 사전집에 같은 표준어가 둘이면 저장할 수 없다.")
    @Test
    void save_preferredFormIsDuplicated() {
        // given
        Long dictionaryId = saveDictionary(saveWorkspace(), 1, DictionaryStatus.ACTIVE);
        saveTerm(dictionaryId, "회원");

        // when & then
        assertThatThrownBy(() -> {
                    saveTerm(dictionaryId, "회원");
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 버전마다 사전집 행이 복제되면서 용어도 함께 복제되므로, 같은 표준어가 버전 수만큼 존재하는 것이 정상이다.
     */
    @DisplayName("다른 사전집 버전이라면 같은 표준어를 쓸 수 있다.")
    @Test
    void save_preferredFormIsUniquePerDictionary() {
        // given
        Long workspaceId = saveWorkspace();
        Long archived = saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        Long active = saveDictionary(workspaceId, 2, DictionaryStatus.ACTIVE);
        saveTerm(archived, "회원");

        // when
        saveTerm(active, "회원");
        em.flush();
        em.clear();

        // then
        assertThat(termRepository.findAllByDictionaryIdOrderByPreferredFormAsc(active))
                .extracting(Term::getPreferredForm)
                .containsExactly("회원");
    }

    private Term saveTerm(Long dictionaryId, String preferredForm) {
        return termRepository.save(TermFixture.term()
                .dictionaryId(dictionaryId)
                .preferredForm(preferredForm)
                .build());
    }

    private Long saveDictionary(Long workspaceId, int versionNo, DictionaryStatus status) {
        Dictionary dictionary = dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(versionNo)
                .status(status)
                .build());

        return dictionary.getId();
    }

    private Long saveWorkspace() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());

        return workspace.getId();
    }
}
