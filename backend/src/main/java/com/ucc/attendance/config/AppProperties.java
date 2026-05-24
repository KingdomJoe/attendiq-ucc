package com.ucc.attendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Qr qr,
        String studentEmailPattern,
        String corsOrigins
) {
    public record Jwt(String secret, int expiryMinutes) {}
    public record Qr(int ttlSeconds) {}
}
