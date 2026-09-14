package com.ubidict.backend.workspace.implement;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InvitationTokenGenerator {

    public String generate() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
