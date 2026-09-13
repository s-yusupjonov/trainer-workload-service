package com.gym.workload.config;

import com.gym.workload.security.JwtAuthenticationEntryPoint;
import com.gym.workload.security.JwtAuthenticationFilter;
import com.gym.workload.security.JwtTokenValidator;
import com.gym.workload.security.SecurityErrorResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenValidator jwtTokenValidator;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtTokenValidator jwtTokenValidator,
                           SecurityErrorResponseWriter securityErrorResponseWriter,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenValidator, securityErrorResponseWriter),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
