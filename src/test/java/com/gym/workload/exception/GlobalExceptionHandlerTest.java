package com.gym.workload.exception;

import com.gym.workload.dto.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void handleUnexpectedException_hidesDetailAndIncludesTransactionId() {
        MDC.put("transactionId", "txn-999");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");

        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(new RuntimeException("db password leaked"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("db password");
        assertThat(response.getBody().getTransactionId()).isEqualTo("txn-999");
        assertThat(response.getBody().getPath()).isEqualTo("/api/trainer-workloads/trainer.one");
    }

    @Test
    void handleUnexpectedException_withoutTransactionIdInMdcYieldsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");

        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(new RuntimeException("boom"), request);

        assertThat(response.getBody().getTransactionId()).isNull();
    }

    @Test
    void handleResourceNotFoundException_returnsNotFoundWithTransactionId() {
        MDC.put("transactionId", "txn-1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/unknown");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("No workload recorded for trainer 'unknown'"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getTransactionId()).isEqualTo("txn-1");
    }

    @Test
    void handleInvalidRequestException_returnsBadRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one/2026/13");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequestException(
                new InvalidRequestException("Month must be between 1 and 12"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Month must be between 1 and 12");
    }

    @Test
    void handleTypeMismatch_returnsBadRequestWithParameterName() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one/2026/abc");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "month", null, new NumberFormatException("abc"));

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("month");
    }
}
