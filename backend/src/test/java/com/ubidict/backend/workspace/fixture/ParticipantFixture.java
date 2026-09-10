package com.ubidict.backend.workspace.fixture;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 소유자 외의 권한은 운영 코드에 등록 경로가 아직 없으므로(REQ-WS-003·004) 리플렉션으로 주입한다.
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
