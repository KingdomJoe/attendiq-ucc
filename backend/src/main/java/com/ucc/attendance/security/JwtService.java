package com.ucc.attendance.security;

import com.ucc.attendance.config.AppProperties;
import com.ucc.attendance.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(AppProperties appProperties) {
        byte[] secretBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiryMinutes = appProperties.jwt().expiryMinutes();
    }

    public String generateToken(Long userId, String subject, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiryMinutes * 60);
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String createQrToken(Long sessionId, String courseCode, String nonce, Instant expiresAt) {
        return Jwts.builder()
                .claims(Map.of(
                        "sessionId", sessionId,
                        "courseCode", courseCode,
                        "nonce", nonce,
                        "type", "QR"
                ))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Claims parseQrClaims(String token) {
        Claims claims = parseClaims(token);
        if (!"QR".equals(claims.get("type"))) {
            throw new IllegalArgumentException("Invalid QR token");
        }
        return claims;
    }
}
