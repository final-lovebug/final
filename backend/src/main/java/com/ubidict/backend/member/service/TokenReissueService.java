package com.ubidict.backend.member.service;

import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.implement.TokenRefresher;
import com.ubidict.backend.member.service.model.TokenPairResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TokenReissueService {

    private final TokenRefresher tokenRefresher;

    @Transactional
    public TokenPairResult reissue(String refreshToken) {
        TokenPair tokenPair = tokenRefresher.refresh(refreshToken);
        return new TokenPairResult(tokenPair.accessToken(), tokenPair.refreshToken(), tokenPair.role());
    }
}
