package com.ubidict.backend.reviewrequest.implement;

import org.springframework.stereotype.Component;

@Component
public class ReexamineRoundCalculator {

    public int nextRound(int currentRound) {
        if (currentRound < 0) {
            throw new IllegalArgumentException("currentRound must not be negative");
        }
        return Math.incrementExact(currentRound);
    }
}
