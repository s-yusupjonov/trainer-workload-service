package com.gym.workload.logging;

import com.gym.workload.dto.WorkloadEventRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OperationLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLoggingAspect.class);

    @Around("execution(* com.gym.workload.controller.TrainerWorkloadController.*(..))")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        String requestSummary = buildSummary(operation, joinPoint.getArgs());
        log.info("operation called endpoint={} request={}", operation, requestSummary);

        try {
            Object result = joinPoint.proceed();
            log.info("operation outcome endpoint={} status={} message=success", operation, extractStatus(result));
            return result;
        } catch (Exception ex) {
            log.warn("operation outcome endpoint={} outcome=failed reason={}", operation, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private String buildSummary(String operation, Object[] args) {
        return switch (operation) {
            case "recordEvent" -> summarizeRecordEvent(args);
            case "getSummary" -> summarizeUsernameOnly(args);
            case "getMonthSummary" -> summarizeMonthLookup(args);
            default -> "unrecognized operation";
        };
    }

    private String summarizeRecordEvent(Object[] args) {
        if (args.length == 0 || !(args[0] instanceof WorkloadEventRequest request)) {
            return "trainerUsername=unknown";
        }
        return "trainerUsername=" + request.getTrainerUsername()
                + " actionType=" + request.getActionType()
                + " trainingDate=" + request.getTrainingDate();
    }

    private String summarizeUsernameOnly(Object[] args) {
        if (args.length == 0) {
            return "trainerUsername=unknown";
        }
        return "trainerUsername=" + args[0];
    }

    private String summarizeMonthLookup(Object[] args) {
        if (args.length < 3) {
            return "trainerUsername=unknown";
        }
        return "trainerUsername=" + args[0] + " year=" + args[1] + " month=" + args[2];
    }

    private int extractStatus(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return 0;
    }
}
