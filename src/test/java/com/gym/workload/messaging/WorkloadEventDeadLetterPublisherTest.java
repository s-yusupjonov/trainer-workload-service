package com.gym.workload.messaging;

import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadEventDeadLetterPublisherTest {

    private static final String DLQ = "workload.events.dlq";

    @Mock
    private JmsTemplate jmsTemplate;

    private WorkloadEventDeadLetterPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WorkloadEventDeadLetterPublisher(jmsTemplate, DLQ);
    }

    @Test
    void publishShouldSendOriginalPayloadWithReasonToDeadLetterQueue() throws Exception {
        publisher.publish("{\"trainerUsername\":null}", "trainerUsername must not be blank");

        ArgumentCaptor<MessageCreator> creatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);
        verify(jmsTemplate).send(eq(DLQ), creatorCaptor.capture());

        Session session = mock(Session.class);
        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);

        creatorCaptor.getValue().createMessage(session);

        verify(session).createTextMessage("{\"trainerUsername\":null}");
        verify(textMessage).setStringProperty("rejectionReason", "trainerUsername must not be blank");
    }
}
