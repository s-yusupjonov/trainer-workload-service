package com.gym.workload.security;

import com.gym.workload.support.TestJwtSupport;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private final JwtTokenValidator validator =
            new JwtTokenValidator(TestJwtSupport.SECRET, TestJwtSupport.ALLOWED_CALLER);

    @Test
    void validateAndExtractCaller_validTokenReturnsSubject() {
        String subject = validator.validateAndExtractCaller(TestJwtSupport.validToken());

        assertThat(subject).isEqualTo(TestJwtSupport.ALLOWED_CALLER);
    }

    @Test
    void validateAndExtractCaller_expiredTokenThrows() {
        String expired = TestJwtSupport.expiredToken();

        assertThatThrownBy(() -> validator.validateAndExtractCaller(expired))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractCaller_malformedTokenThrows() {
        assertThatThrownBy(() -> validator.validateAndExtractCaller("not-a-token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void isAllowedCaller_trueForConfiguredCaller() {
        assertThat(validator.isAllowedCaller(TestJwtSupport.ALLOWED_CALLER)).isTrue();
    }

    @Test
    void isAllowedCaller_falseForUnknownCaller() {
        assertThat(validator.isAllowedCaller("some-other-service")).isFalse();
    }

    @Test
    void isAllowedCaller_falseForNullCaller() {
        assertThat(validator.isAllowedCaller(null)).isFalse();
    }

    @Test
    void isAllowedCaller_supportsMultipleCallersInAllowList() {
        JwtTokenValidator multiCallerValidator =
                new JwtTokenValidator(TestJwtSupport.SECRET, " gym-crm , discovery-service ,, ");

        assertThat(multiCallerValidator.isAllowedCaller("gym-crm")).isTrue();
        assertThat(multiCallerValidator.isAllowedCaller("discovery-service")).isTrue();
        assertThat(multiCallerValidator.isAllowedCaller("unknown")).isFalse();
    }

    @Test
    void validateAndExtractCaller_toleratesDurationArgument() {
        String subject = validator.validateAndExtractCaller(
                TestJwtSupport.validToken(TestJwtSupport.ALLOWED_CALLER, Duration.ofSeconds(30)));

        assertThat(subject).isEqualTo(TestJwtSupport.ALLOWED_CALLER);
    }
}
