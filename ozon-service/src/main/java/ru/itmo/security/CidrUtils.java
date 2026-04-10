package ru.itmo.security;

import org.springframework.security.web.util.matcher.IpAddressMatcher;

public final class CidrUtils {

    private CidrUtils() {
    }

    public static void validateIpv4Cidr(String cidr) {
        if (cidr == null || cidr.isBlank()) {
            throw new IllegalArgumentException("CIDR is required");
        }
        String t = cidr.trim();
        if (t.contains(":")) {
            throw new IllegalArgumentException("Only IPv4 CIDR is supported");
        }
        try {
            new IpAddressMatcher(t);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IPv4 CIDR: " + cidr, e);
        }
    }

    public static int prefixLength(String cidr) {
        int i = cidr.lastIndexOf('/');
        return Integer.parseInt(cidr.substring(i + 1).trim());
    }

    public static boolean matches(String cidr, String ip) {
        validateIpv4Cidr(cidr);
        return new IpAddressMatcher(cidr.trim()).matches(ip);
    }
}
