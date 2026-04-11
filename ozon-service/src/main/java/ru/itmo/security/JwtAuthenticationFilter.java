package ru.itmo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/swagger-ui.html")) {
            return true;
        }
        if (path.startsWith("/api/public/")) {
            return true;
        }
        return path.equals("/api/auth/login") && "POST".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        String currentIp = clientIpResolver.resolve(request);
        try {
            JwtService.JwtPayload payload = jwtService.parseAndValidateIp(token, currentIp);
            List<SimpleGrantedAuthority> auths = payload.privileges().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            AppUserPrincipal principal = new AppUserPrincipal(
                    payload.pickupPointId(),
                    payload.username(),
                    "",
                    auths
            );
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, auths);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtService.JwtIpMismatchException e) {
            SecurityContextHolder.clearContext();
            writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_IP_REVOKED", e.getMessage());
            return;
        } catch (JwtService.JwtRevokedException e) {
            SecurityContextHolder.clearContext();
            writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_REVOKED", e.getMessage());
            return;
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeAuthError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
