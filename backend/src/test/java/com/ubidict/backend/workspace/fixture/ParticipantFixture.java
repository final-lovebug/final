package com.ubidict.backend.workspace.fixture;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 운영 코드에는 초대와 권한 변경 경로가 있다. 저장 전 상태를 독립적으로 만들기 위해 리플렉션으로 권한을 주입한다.
 */
public class ParticipantFixture {

    public static ParticipantBuilder participant() {
        return new ParticipantBuilder();
    }

    public static class ParticipantBuilder {

        private Long id;
        private Long workspaceId = 1L;
        private Long memberId = 1L;
        private Permission permission = Permission.OWNER;

        public ParticipantBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ParticipantBuilder workspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public ParticipantBuilder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public ParticipantBuilder permission(Permission permission) {
            this.permission = permission;
            return this;
        }

        public Participant build() {
            Participant participant = Participant.owner(workspaceId, memberId);
            if (id != null) {
                ReflectionTestUtils.setField(participant, "id", id);
            }
            if (permission != Permission.OWNER) {
                ReflectionTestUtils.setField(participant, "permission", permission);
            }

            return participant;
        }
    }
}
