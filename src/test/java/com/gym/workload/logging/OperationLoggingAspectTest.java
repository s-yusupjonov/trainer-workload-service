package com.gym.workload.logging;

import com.gym.workload.dto.request.ActionType;
import com.gym.workload.dto.request.WorkloadEventRequest;
import com.gym.workload.exception.ResourceNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationLoggingAspectTest {

    private final OperationLoggingAspect aspect = new OperationLoggingAspect();

    private ProceedingJoinPoint mockJoinPoint(String methodName, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        given(signature.getName()).willReturn(methodName);
        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getArgs()).willReturn(args);
        return joinPoint;
    }

    @Test
    void logOperation_recordEvent_returnsProceedResult() throws Throwable {
        WorkloadEventRequest request = new WorkloadEventRequest();
        request.setTrainerUsername("trainer.one");
        request.setTrainingDate(LocalDate.of(2026, 8, 1));
        request.setActionType(ActionType.ADD);
        ProceedingJoinPoint joinPoint = mockJoinPoint("recordEvent", new Object[] { request });
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        Object result = aspect.logOperation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        verify(joinPoint).proceed();
    }

    @Test
    void logOperation_recordEvent_handlesNonRequestArg() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("recordEvent", new Object[] {});
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        Object result = aspect.logOperation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
    }

    @Test
    void logOperation_getSummary_returnsProceedResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("getSummary", new Object[] { "trainer.one" });
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        Object result = aspect.logOperation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
    }

    @Test
    void logOperation_getMonthSummary_returnsProceedResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("getMonthSummary", new Object[] { "trainer.one", 2026, 8 });
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        Object result = aspect.logOperation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
    }

    @Test
    void logOperation_unrecognizedOperation_stillProceeds() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("someOtherMethod", new Object[] {});
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        Object result = aspect.logOperation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
    }

    @Test
    void logOperation_nonResponseEntityResult_doesNotFail() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("getSummary", new Object[] { "trainer.one" });
        given(joinPoint.proceed()).willReturn("not-a-response-entity");

        Object result = aspect.logOperation(joinPoint);

        assertThat(result).isEqualTo("not-a-response-entity");
    }

    @Test
    void logOperation_exceptionIsLoggedAndRethrown() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("getSummary", new Object[] { "unknown" });
        given(joinPoint.proceed()).willThrow(new ResourceNotFoundException("not found"));

        assertThatThrownBy(() -> aspect.logOperation(joinPoint))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
