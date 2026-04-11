package ru.itmo.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

public final class RolePrivilegeRegistry {

    private static final Map<String, List<String>> ROLE_PRIVILEGES = Map.of(
            "USER", List.of("ORDER_CREATE", "ORDER_VIEW_OWN", "ORDER_CANCEL_OWN"),
            "PICKUP_POINT_ADMIN", List.of("ORDER_VIEW_PICKUP_POINT", "ORDER_UPDATE_PICKUP_POINT_STATUS"),
            "ADMIN", List.of("ORDER_VIEW_ALL", "ORDER_UPDATE_ALL")
    );

    public static Set<String> privilegeNamesForRoles(Collection<String> roleNames) {
        Set<String> out = new LinkedHashSet<>();
        for (String role : roleNames) {
            out.addAll(ROLE_PRIVILEGES.getOrDefault(role, List.of()));
        }
        return out;
    }
}
