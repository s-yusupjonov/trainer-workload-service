package com.gym.workload.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gym.workload.dto.request.WorkloadEventRequest;
import com.gym.workload.service.TrainerWorkloadService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkloadEventListenerTest {

    @Mock
    private TrainerWorkloadService trainerWorkloadService;

    @Mock
    private WorkloadEventDeadLetterPublisher deadLetterPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private WorkloadEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new WorkloadEventListener(trainerWorkloadService, objectMapper, validator, deadLetterPublisher);
    }

    @Test
    void onMessageShouldRecordEventWhenPayloadIsValid() throws JMSException {
        String payload = """
                {"trainerUsername":"trainer.one","trainerFirstName":"John","trainerLastName":"Doe",
                "isActive":true,"trainingDate":"2026-08-01","trainingDuration":60,"actionType":"ADD"}""";
        TextMessage message = textMessage(payload, "txn-123");

        listener.onMessage(message);

        ArgumentCaptor<WorkloadEventRequest> requestCaptor = ArgumentCaptor.forClass(WorkloadEventRequest.class);
        verify(trainerWorkloadService).recordEvent(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTrainerUsername()).isEqualTo("trainer.one");
        verify(deadLetterPublisher, never()).publish(any(), any());
    }

    @Test
    void onMessageShouldRouteToDeadLetterQueueWhenRequiredInformationIsMissing() throws JMSException {
        String payload = """
                {"trainerUsername":"","trainerFirstName":"John","trainerLastName":"Doe",
                "isActive":true,"trainingDate":"2026-08-01","trainingDuration":60,"actionType":"ADD"}""";
        TextMessage message = textMessage(payload, "txn-456");

        listener.onMessage(message);

        verify(trainerWorkloadService, never()).recordEvent(any());
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(deadLetterPublisher).publish(org.mockito.ArgumentMatchers.eq(payload), reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).contains("trainerUsername");
    }

    @Test
    void onMessageShouldRouteToDeadLetterQueueWhenPayloadIsMalformed() throws JMSException {
        String payload = "not valid json";
        TextMessage message = textMessage(payload, "txn-789");

        listener.onMessage(message);

        verify(trainerWorkloadService, never()).recordEvent(any());
        verify(deadLetterPublisher).publish(payload, "Malformed JSON payload");
    }

    @Test
    void onMessageShouldRejectNonTextMessages() {
        Message message = org.mockito.Mockito.mock(Message.class);

        assertThatThrownBy(() -> listener.onMessage(message)).isInstanceOf(JMSException.class);
        verify(trainerWorkloadService, never()).recordEvent(any());
    }

    private TextMessage textMessage(String payload, String transactionId) throws JMSException {
        TextMessage message = org.mockito.Mockito.mock(TextMessage.class);
        given(message.getText()).willReturn(payload);
        given(message.getStringProperty("transactionId")).willReturn(transactionId);
        return message;
    }
}
