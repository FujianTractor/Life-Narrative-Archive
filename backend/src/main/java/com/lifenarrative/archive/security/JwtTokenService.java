package com.lifenarrative.archive.security;

import com.lifenarrative.archive.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;

    public JwtTokenService(@Value("${app.security.jwt-secret}") String rawSecret) {
        this.signingKey = Keys.hmacShaKeyFor(hashSecret(rawSecret));
    }

    public String createToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("displayName", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(12, ChronoUnit.HOURS)))
                .signWith(signingKey)
                .compact();
    }

    public ArchivePrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new ArchivePrincipal(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("displayName", String.class)
        );
    }

    private byte[] hashSecret(String rawSecret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(rawSecret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}