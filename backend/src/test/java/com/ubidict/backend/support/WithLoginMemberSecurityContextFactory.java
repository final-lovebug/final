package com.ubidict.backend.support;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/** {@link WithLoginMember}가 선언한 회원으로 SecurityContext 를 만든다. */
public class WithLoginMemberSecurityContextFactory implements WithSecurityContextFactory<WithLoginMember> {

    @Override
    public SecurityContext createSecurityContext(WithLoginMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                annotation.value(), null, List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()))));

        return context;
    }
}
