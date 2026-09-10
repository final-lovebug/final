package com.ubidict.backend.member.service.model;

import com.ubidict.backend.member.domain.OAuthProvider;

public record CreateMemberCommand(String email, String displayName, OAuthProvider provider, String providerId) {}
