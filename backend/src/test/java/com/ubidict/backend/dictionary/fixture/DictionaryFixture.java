package com.ubidict.backend.dictionary.fixture;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.DictionaryVersion;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 저장 전 식별자는 null이므로, 식별자가 검증에 필요한 테스트에서만 builder로 주입한다.
 *
 * <p>보관 상태와 임의의 버전 번호는 운영 코드에서 새 버전 반영을 거쳐야만 나오므로 리플렉션으로 주입한다.
 */
public class DictionaryFixture {

    public static DictionaryBuilder dictionary() {
        return new DictionaryBuilder();
    }

    public static class DictionaryBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private Long createdBy = 1L;
        private Integer versionNo;
        private DictionaryStatus status = DictionaryStatus.ACTIVE;

        public DictionaryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DictionaryBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public DictionaryBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public DictionaryBuilder versionNo(int versionNo) {
            this.versionNo = versionNo;
            return this;
        }

        public DictionaryBuilder status(DictionaryStatus status) {
            this.status = status;
            return this;
        }

        public Dictionary build() {
            Dictionary dictionary = Dictionary.createFirst(workspaceId, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(dictionary, "id", id);
            }
            if (versionNo != null) {
                DictionaryVersion version =
                        new DictionaryVersion(versionNo, dictionary.getVersion().publishedAt());
                ReflectionTestUtils.setField(dictionary, "version", version);
            }
            if (status == DictionaryStatus.ARCHIVED) {
                dictionary.archive();
            }

            return dictionary;
        }
    }
}
