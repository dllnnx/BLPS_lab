package ru.itmo.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.dto.auth.LoginRequest;
import ru.itmo.dto.auth.LoginResponse;
import ru.itmo.models.AppUser;
import ru.itmo.repositories.AppUserRepository;
import ru.itmo.security.ClientIpResolver;
import ru.itmo.security.JwtService;
import ru.itmo.security.RolePrivilegeRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final NetworkPolicyAccessService networkPolicyAccessService;
    private final JwtService jwtService;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }
        String clientIp = clientIpResolver.resolve(httpRequest);
        Set<String> effectiveRoles = networkPolicyAccessService.resolveEffectiveRoleNames(user.getRoles(), clientIp);
        if (effectiveRoles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No effective roles for this IP (check network policies)"
            );
        }
        Set<String> privileges = RolePrivilegeRegistry.privilegeNamesForRoles(effectiveRoles);
        String token = jwtService.createToken(
                user.getUsername(),
                clientIp,
                new ArrayList<>(privileges),
                user.getPickupPointId()
        );
        return new LoginResponse(
                token,
                jwtExpirationMs,
                new LinkedHashSet<>(effectiveRoles),
                privileges
        );
    }
}
