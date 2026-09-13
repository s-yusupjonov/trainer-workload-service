package com.gym.workload.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseWriterTest {

    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(new ObjectMapper());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void write_producesJsonErrorBodyWithTransactionId() throws Exception {
        MDC.put("transactionId", "txn-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, HttpStatus.UNAUTHORIZED, "Missing token", "/api/trainer-workloads/trainer.one");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("txn-123", "Missing token", "/api/trainer-workloads/trainer.one");
    }

    @Test
    void write_worksWithoutTransactionIdInMdc() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, HttpStatus.FORBIDDEN, "Caller not permitted", "/api/trainer-workloads");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Caller not permitted");
    }
}
