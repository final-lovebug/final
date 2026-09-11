package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Participant;
import org.springframework.stereotype.Component;

@Component
public class ParticipantRemover {
    public void remove(Participant participant) {
        participant.delete();
    }
}
