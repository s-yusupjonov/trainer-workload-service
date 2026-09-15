package com.gym.workload.messaging;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkloadEventDeadLetterPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkloadEventDeadLetterPublisher.class);

    static final String REJECTION_REASON_PROPERTY = "rejectionReason";

    private final JmsTemplate jmsTemplate;
    private final String deadLetterQueue;

    public WorkloadEventDeadLetterPublisher(JmsTemplate jmsTemplate,
                                            @Value("${activemq.queue.workload-events-dlq}") String deadLetterQueue) {
        this.jmsTemplate = jmsTemplate;
        this.deadLetterQueue = deadLetterQueue;
    }

    public void publish(String originalPayload, String reason) {
        jmsTemplate.send(deadLetterQueue, session -> buildMessage(session, originalPayload, reason));
        log.warn("Routed invalid workload event to dead letter queue: queue={} reason={}", deadLetterQueue, reason);
    }

    private TextMessage buildMessage(Session session, String payload, String reason) throws JMSException {
        TextMessage message = session.createTextMessage(payload);
        message.setStringProperty(REJECTION_REASON_PROPERTY, reason);
        return message;
    }
}
