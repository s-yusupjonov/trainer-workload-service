package com.gym.workload.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationEntryPointTest {

    @Test
    void commence_delegatesToErrorResponseWriter() throws Exception {
        SecurityErrorResponseWriter errorResponseWriter = mock(SecurityErrorResponseWriter.class);
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(errorResponseWriter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("no auth"));

        verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED,
                "Missing or invalid authentication token", "/api/trainer-workloads/trainer.one");
    }
}
