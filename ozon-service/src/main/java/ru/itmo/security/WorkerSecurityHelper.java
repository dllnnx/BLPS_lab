package ru.itmo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkerSecurityHelper {

    private final AppUserDetailsService appUserDetailsService;

    public void authenticateAs(String username) {
        UserDetails user = appUserDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    public void clear() {
        SecurityContextHolder.clearContext();
    }
}
