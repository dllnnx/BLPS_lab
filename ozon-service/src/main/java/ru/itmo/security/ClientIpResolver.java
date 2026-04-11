package ru.itmo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split("\\s*,\\s*")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
