package com.gym.workload.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String API_PREFIX = "/api/";

    private final JwtTokenValidator tokenValidator;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator, SecurityErrorResponseWriter errorResponseWriter) {
        this.tokenValidator = tokenValidator;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        try {
            String caller = tokenValidator.validateAndExtractCaller(token);
            if (!tokenValidator.isAllowedCaller(caller)) {
                errorResponseWriter.write(response, HttpStatus.FORBIDDEN,
                        "Caller is not permitted to access this service", request.getRequestURI());
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    caller, null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token", request.getRequestURI());
        }
    }
}
