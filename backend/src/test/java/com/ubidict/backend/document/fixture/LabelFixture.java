package com.ubidict.backend.document.fixture;

import com.ubidict.backend.document.domain.Label;
import org.springframework.test.util.ReflectionTestUtils;

public class LabelFixture {

    public static LabelBuilder label() {
        return new LabelBuilder();
    }

    public static class LabelBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private String name = "설계";
        private Long createdBy = 1L;

        public LabelBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabelBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public LabelBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LabelBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Label build() {
            Label label = Label.create(workspaceId, name, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(label, "id", id);
            }

            return label;
        }
    }
}
