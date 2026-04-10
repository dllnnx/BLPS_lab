package ru.itmo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Определение «клиентского» IP: опциональный доверенный заголовок (подмена для тестов),
 * затем X-Forwarded-For (первый hop), иначе remoteAddr.
 */
@Component
public class ClientIpResolver {

    @Value("${app.security.use-client-ip-header:false}")
    private boolean useClientIpHeader;

    @Value("${app.security.client-ip-header:X-Client-IP}")
    private String clientIpHeader;

    public String resolve(HttpServletRequest request) {
        if (useClientIpHeader) {
            String fromHeader = request.getHeader(clientIpHeader);
            if (StringUtils.hasText(fromHeader)) {
                return fromHeader.trim().split("\\s*,\\s*")[0];
            }
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split("\\s*,\\s*")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
