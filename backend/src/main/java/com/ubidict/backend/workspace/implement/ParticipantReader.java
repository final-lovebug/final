package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantReader {

    private final ParticipantRepository participantRepository;

    public Optional<Participant> readOptional(Long workspaceId, Long memberId) {
        return participantRepository.findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, memberId);
    }

    public List<Participant> readAllByMember(Long memberId) {
        return participantRepository.findAllByMemberIdAndDeletedAtIsNull(memberId);
    }
}
