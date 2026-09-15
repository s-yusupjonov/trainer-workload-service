package com.gym.workload.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.workload.dto.request.WorkloadEventRequest;
import com.gym.workload.logging.TransactionIdFilter;
import com.gym.workload.service.TrainerWorkloadService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class WorkloadEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkloadEventListener.class);

    static final String TRANSACTION_ID_PROPERTY = "transactionId";

    private final TrainerWorkloadService trainerWorkloadService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final WorkloadEventDeadLetterPublisher deadLetterPublisher;

    public WorkloadEventListener(TrainerWorkloadService trainerWorkloadService,
                                 ObjectMapper objectMapper,
                                 Validator validator,
                                 WorkloadEventDeadLetterPublisher deadLetterPublisher) {
        this.trainerWorkloadService = trainerWorkloadService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @JmsListener(destination = "${activemq.queue.workload-events}")
    public void onMessage(Message message) throws JMSException {
        MDC.put(TransactionIdFilter.MDC_KEY, resolveTransactionId(message));
        try {
            handle(extractPayload(message));
        } finally {
            MDC.remove(TransactionIdFilter.MDC_KEY);
        }
    }

    private void handle(String payload) {
        WorkloadEventRequest request = tryParse(payload);
        if (request == null) {
            deadLetterPublisher.publish(payload, "Malformed JSON payload");
            return;
        }

        Set<ConstraintViolation<WorkloadEventRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            deadLetterPublisher.publish(payload, describeViolations(violations));
            return;
        }

        trainerWorkloadService.recordEvent(request);
        log.info("Recorded workload event: trainerUsername={} actionType={} trainingDate={}",
                request.getTrainerUsername(), request.getActionType(), request.getTrainingDate());
    }

    private WorkloadEventRequest tryParse(String payload) {
        try {
            return objectMapper.readValue(payload, WorkloadEventRequest.class);
        } catch (Exception ex) {
            log.warn("Failed to parse workload event payload: {}", ex.getMessage());
            return null;
        }
    }

    private String describeViolations(Set<ConstraintViolation<WorkloadEventRequest>> violations) {
        String reason = violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Rejected invalid workload event: {}", reason);
        return reason;
    }

    private String extractPayload(Message message) throws JMSException {
        if (message instanceof TextMessage textMessage) {
            return textMessage.getText();
        }
        throw new JMSException("Unsupported JMS message type: " + message.getClass().getName());
    }

    private String resolveTransactionId(Message message) throws JMSException {
        String transactionId = message.getStringProperty(TRANSACTION_ID_PROPERTY);
        return (transactionId != null && !transactionId.isBlank()) ? transactionId : UUID.randomUUID().toString();
    }
}
