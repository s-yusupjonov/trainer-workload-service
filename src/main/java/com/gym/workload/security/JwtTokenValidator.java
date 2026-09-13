package com.gym.workload.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenValidator {

    private final SecretKey secretKey;
    private final Set<String> allowedCallers;

    public JwtTokenValidator(
            @Value("${internal-auth.secret}") String secret,
            @Value("${internal-auth.allowed-callers}") String allowedCallersProperty) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.allowedCallers = Arrays.stream(allowedCallersProperty.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String validateAndExtractCaller(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean isAllowedCaller(String caller) {
        return caller != null && allowedCallers.contains(caller);
    }
}
