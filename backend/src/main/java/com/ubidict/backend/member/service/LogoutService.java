package com.ubidict.backend.member.service;

import com.ubidict.backend.member.implement.TokenRevoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LogoutService {

    private final TokenRevoker tokenRevoker;

    @Transactional
    public void logout(String refreshToken) {
        tokenRevoker.revoke(refreshToken);
    }
}
