package com.example.videoplatform.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE = "tokenType";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, ACCESS, accessTokenExpirationSeconds);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, REFRESH, refreshTokenExpirationSeconds);
    }

    public Long getAccessTokenUserId(String token) {
        return getUserId(token, ACCESS);
    }

    public Long getRefreshTokenUserId(String token) {
        return getUserId(token, REFRESH);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private String createToken(Long userId, String tokenType, long expirationSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    private Long getUserId(String token, String expectedType) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
        if (!expectedType.equals(claims.get(TOKEN_TYPE, String.class))) {
            throw new IllegalArgumentException("Unexpected token type");
        }
        return Long.valueOf(claims.getSubject());
    }
}
