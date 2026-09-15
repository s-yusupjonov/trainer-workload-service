package com.gym.workload.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIdFilterTest {

    private final TransactionIdFilter filter = new TransactionIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void missingHeader_generatesNewTransactionIdAndEchoesIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(TransactionIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain.get()).isNotBlank();
        assertThat(response.getHeader(TransactionIdFilter.TRANSACTION_ID_HEADER)).isEqualTo(mdcDuringChain.get());
        assertThat(MDC.get(TransactionIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void existingHeader_isReusedAndEchoedBack() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");
        request.addHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, "given-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(TransactionIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain.get()).isEqualTo("given-id-123");
        assertThat(response.getHeader(TransactionIdFilter.TRANSACTION_ID_HEADER)).isEqualTo("given-id-123");
    }

    @Test
    void blankHeader_generatesNewTransactionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");
        request.addHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(TransactionIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain.get()).isNotBlank().isNotEqualTo("   ");
    }

    @Test
    void mdcIsClearedEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/trainer.one");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
        }

        assertThat(MDC.get(TransactionIdFilter.MDC_KEY)).isNull();
    }
}
