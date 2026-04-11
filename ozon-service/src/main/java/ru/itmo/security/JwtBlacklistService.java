package ru.itmo.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Отзыв JWT после нарушения привязки к IP: токен с этим jti больше не принимается до истечения срока.
 */
@Service
public class JwtBlacklistService {

    private final ConcurrentHashMap<String, Long> revokedUntilEpochMs = new ConcurrentHashMap<>();

    public void revokeUntil(String key, long expEpochMs) {
        if (key == null || key.isBlank()) {
            return;
        }
        revokedUntilEpochMs.merge(key, expEpochMs, Math::max);
    }

    public boolean isRevoked(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        Long until = revokedUntilEpochMs.get(key);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            revokedUntilEpochMs.remove(key, until);
            return false;
        }
        return true;
    }
}
