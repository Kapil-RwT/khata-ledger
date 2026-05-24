package com.khataledger.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * Tiny JWT helper. Issues HS256 tokens carrying the merchant id as the subject
 * plus business name as a custom claim.
 *
 * Interview talking points:
 *  - "We use HS256 because this app issues and verifies its own tokens (no third-party IdP).
 *     For OIDC scenarios we'd switch to RS256 with a JWK Set."
 *  - "Token expiry is short (60 min). For a real product we'd add a refresh-token flow."
 *  - "Secret comes from env via @Value; never hardcoded."
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes) {
        // accept either a raw string (must be >= 32 bytes) or a base64 secret
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(Long merchantId, String businessName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(merchantId))
                .claims(Map.of("businessName", businessName))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
