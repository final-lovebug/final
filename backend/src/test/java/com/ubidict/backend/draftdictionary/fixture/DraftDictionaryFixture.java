package com.ubidict.backend.draftdictionary.fixture;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public class DraftDictionaryFixture {

    public static DraftDictionaryBuilder draftDictionary() {
        return new DraftDictionaryBuilder();
    }

    public static class DraftDictionaryBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private Long dictionaryId;
        private List<Long> sourceDocumentIds = List.of(10L);
        private DraftDictionaryStatus status = DraftDictionaryStatus.EXAMINING;
        private Long createdBy = 2L;

        public DraftDictionaryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DraftDictionaryBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public DraftDictionaryBuilder dictionaryId(Long dictionaryId) {
            this.dictionaryId = dictionaryId;
            return this;
        }

        public DraftDictionaryBuilder sourceDocumentIds(List<Long> sourceDocumentIds) {
            this.sourceDocumentIds = sourceDocumentIds;
            return this;
        }

        public DraftDictionaryBuilder status(DraftDictionaryStatus status) {
            this.status = status;
            return this;
        }

        public DraftDictionaryBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public DraftDictionary build() {
            DraftDictionary draftDictionary =
                    DraftDictionary.create(workspaceId, dictionaryId, sourceDocumentIds, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(draftDictionary, "id", id);
            }
            if (status != DraftDictionaryStatus.EXAMINING) {
                ReflectionTestUtils.setField(draftDictionary, "status", status);
            }

            return draftDictionary;
        }
    }
}
