package ru.itmo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.itmo.models.AppUser;
import ru.itmo.repositories.AppUserRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private static final Map<String, List<String>> ROLE_PRIVILEGES = Map.of(
            "USER", List.of("ORDER_CREATE", "ORDER_VIEW_OWN", "ORDER_CANCEL_OWN"),
            "PICKUP_POINT_ADMIN", List.of("ORDER_VIEW_PICKUP_POINT", "ORDER_UPDATE_PICKUP_POINT_STATUS"),
            "ADMIN", List.of("ORDER_VIEW_ALL", "ORDER_UPDATE_ALL")
    );

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String role : user.getRoles()) {
            for (String p : ROLE_PRIVILEGES.getOrDefault(role, List.of())) {
                authorities.add(new SimpleGrantedAuthority(p));
            }
        }
        return new AppUserPrincipal(
                user.getPickupPointId(),
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
