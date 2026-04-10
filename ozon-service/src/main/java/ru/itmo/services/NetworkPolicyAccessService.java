package ru.itmo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.models.NetworkPolicy;
import ru.itmo.models.NetworkPolicyCidr;
import ru.itmo.repositories.NetworkPolicyRepository;
import ru.itmo.security.CidrUtils;

import java.util.*;

/**
 * Вычисление эффективных ролей по IPv4 и сетевым политикам (наибольшая маска / длина префикса побеждает).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkPolicyAccessService {

    private final NetworkPolicyRepository networkPolicyRepository;

    @Transactional(readOnly = true)
    public Set<String> resolveEffectiveRoleNames(Set<String> assignedRoles, String clientIp) {
        if (assignedRoles == null || assignedRoles.isEmpty()) {
            return Set.of();
        }
        List<NetworkPolicy> policies = networkPolicyRepository.findAll();

        boolean anyPolicyTouchesUserRole = policies.stream()
                .anyMatch(p -> p.getRoleNames().stream().anyMatch(assignedRoles::contains));
        if (!anyPolicyTouchesUserRole) {
            log.info("RBAC/CIDR: user roles {} are not covered by any network policy — denying effective roles", assignedRoles);
            return Set.of();
        }

        record ScoredPolicy(NetworkPolicy policy, int bestPrefix) {}

        List<ScoredPolicy> matched = new ArrayList<>();
        for (NetworkPolicy p : policies) {
            if (p.getRoleNames().stream().noneMatch(assignedRoles::contains)) {
                continue;
            }
            if (p.getAddresses() == null || p.getAddresses().isEmpty()) {
                continue;
            }
            int best = -1;
            for (NetworkPolicyCidr row : p.getAddresses()) {
                try {
                    if (CidrUtils.matches(row.getCidr(), clientIp)) {
                        best = Math.max(best, CidrUtils.prefixLength(row.getCidr()));
                    }
                } catch (IllegalArgumentException ex) {
                    log.warn("Skipping invalid CIDR in policy {}: {}", p.getName(), row.getCidr());
                }
            }
            if (best >= 0) {
                matched.add(new ScoredPolicy(p, best));
            }
        }

        if (matched.isEmpty()) {
            log.info("RBAC/CIDR: IP {} matches no CIDR for user's policy-linked roles {} — denying", clientIp, assignedRoles);
            return Set.of();
        }

        int maxPrefix = matched.stream().mapToInt(ScoredPolicy::bestPrefix).max().orElseThrow();
        Set<String> union = new LinkedHashSet<>();
        for (ScoredPolicy sp : matched) {
            if (sp.bestPrefix == maxPrefix) {
                for (String r : sp.policy.getRoleNames()) {
                    if (assignedRoles.contains(r)) {
                        union.add(r);
                    }
                }
            }
        }

        log.info("RBAC/CIDR: clientIp={} assignedRoles={} effectiveRoles={} (winning CIDR prefix length {})",
                clientIp, assignedRoles, union, maxPrefix);
        return union;
    }
}
