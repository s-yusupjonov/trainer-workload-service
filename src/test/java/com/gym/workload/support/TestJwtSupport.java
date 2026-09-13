package com.gym.workload.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class TestJwtSupport {

    public static final String SECRET = "th1s-is-a-shared-dev-only-internal-secret-override-in-prod";
    public static final String ALLOWED_CALLER = "gym-crm";

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private TestJwtSupport() {
    }

    public static String validToken() {
        return validToken(ALLOWED_CALLER, Duration.ofMinutes(5));
    }

    public static String validToken(String subject, Duration timeToLive) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(timeToLive)))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String expiredToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(ALLOWED_CALLER)
                .issuedAt(Date.from(now.minus(Duration.ofMinutes(10))))
                .expiration(Date.from(now.minus(Duration.ofMinutes(5))))
                .signWith(SECRET_KEY)
                .compact();
    }
}
