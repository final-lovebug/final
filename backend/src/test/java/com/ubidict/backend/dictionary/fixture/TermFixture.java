package com.ubidict.backend.dictionary.fixture;

import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import java.util.Arrays;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 저장 전 식별자는 null이므로, 식별자가 검증에 필요한 테스트에서만 builder로 주입한다.
 */
public class TermFixture {

    public static TermBuilder term() {
        return new TermBuilder();
    }

    public static List<NewTermSnapshot> snapshots(String... preferredForms) {
        return Arrays.stream(preferredForms)
                .map(form -> new NewTermSnapshot(form, null, form + "에 대한 정의"))
                .toList();
    }

    public static class TermBuilder {

        private Long id;
        private Long dictionaryId = 1L;
        private String preferredForm = "회원";
        private String englishName = "Member";
        private String definition = "서비스에 가입해 인증받는 주체";
        private Long createdBy = 1L;

        public TermBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TermBuilder dictionaryId(Long dictionaryId) {
            this.dictionaryId = dictionaryId;
            return this;
        }

        public TermBuilder preferredForm(String preferredForm) {
            this.preferredForm = preferredForm;
            return this;
        }

        public TermBuilder englishName(String englishName) {
            this.englishName = englishName;
            return this;
        }

        public TermBuilder definition(String definition) {
            this.definition = definition;
            return this;
        }

        public TermBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Term build() {
            Term term = Term.create(dictionaryId, preferredForm, englishName, definition, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(term, "id", id);
            }

            return term;
        }
    }
}
