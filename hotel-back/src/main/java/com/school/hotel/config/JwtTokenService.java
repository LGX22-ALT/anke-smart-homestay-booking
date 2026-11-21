package com.school.hotel.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenService {
    private final JwtProperties properties;
    public JwtTokenService(JwtProperties properties) { this.properties = properties; }
    private SecretKey key() {
        byte[] bytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(properties.getSecret().getBytes()));
        return Keys.hmacShaKeyFor(bytes);
    }
    public String createAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userId)).claim("role", role).claim("type", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.getAccessExpireMinutes() * 60)))
                .signWith(key()).compact();
    }
    public String createRefreshToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder().id(java.util.UUID.randomUUID().toString()).subject(String.valueOf(userId)).claim("role", role).claim("type", "refresh")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(properties.getRefreshExpireDays() * 86400)))
                .signWith(key()).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload(); }
}
