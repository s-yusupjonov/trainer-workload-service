package com.gym.workload.cucumber.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.workload.cucumber.ScenarioContext;
import com.gym.workload.cucumber.TestApiClient;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for {@code WorkloadEventListener}: publishes raw JSON payloads directly
 * onto the workload-events queue via {@link JmsTemplate} (bypassing the REST layer
 * entirely), then confirms the outcome either by polling the GET endpoint or by reading
 * from the dead-letter queue.
 */
public class WorkloadEventListenerSteps {

    // Matches WorkloadEventDeadLetterPublisher.REJECTION_REASON_PROPERTY (package-private there).
    private static final String REJECTION_REASON_PROPERTY = "rejectionReason";

    private final ScenarioContext scenarioContext;
    private final TestApiClient testApiClient;
    private final JmsTemplate jmsTemplate;
    private final String eventsQueueName;
    private final String deadLetterQueueName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkloadEventListenerSteps(ScenarioContext scenarioContext,
                                      TestApiClient testApiClient,
                                      JmsTemplate jmsTemplate,
                                      @Value("${activemq.queue.workload-events}") String eventsQueueName,
                                      @Value("${activemq.queue.workload-events-dlq}") String deadLetterQueueName) {
        this.scenarioContext = scenarioContext;
        this.testApiClient = testApiClient;
        this.jmsTemplate = jmsTemplate;
        this.eventsQueueName = eventsQueueName;
        this.deadLetterQueueName = deadLetterQueueName;
    }

    @When("a valid ADD workload event is published for trainer {string} with {int} minutes for year {int} month {int}")
    public void aValidAddEventIsPublished(String username, int minutes, int year, int month) {
        publishEvent(username, minutes, year, month, "ADD", null);
    }

    @When("a DELETE workload event is published for trainer {string} with {int} minutes for year {int} month {int}")
    public void aDeleteEventIsPublished(String username, int minutes, int year, int month) {
        publishEvent(username, minutes, year, month, "DELETE", null);
    }

    @When("a malformed workload event missing the {string} field is published for trainer {string}")
    public void aMalformedEventIsPublished(String missingField, String username) {
        publishEvent(username, 30, 2026, 8, "ADD", missingField);
    }

    @Then("the workload summary for trainer {string} should eventually report {int} minutes for year {int} month {int}")
    public void theWorkloadSummaryShouldEventuallyReport(String username, int minutes, int year, int month) {
        String authorizationHeader = scenarioContext.getAuthorizationHeader();
        AtomicReference<ResponseEntity<String>> lastResponse = new AtomicReference<>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    ResponseEntity<String> response = testApiClient.getMonthSummary(
                            username, year, month, authorizationHeader);
                    lastResponse.set(response);
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                    assertThat(objectMapper.readTree(response.getBody()).get("trainingSummaryDuration").asInt())
                            .isEqualTo(minutes);
                });
        scenarioContext.setLastResponse(lastResponse.get());
    }

    @Then("the workload summary for trainer {string} should not be found within {int} seconds")
    public void theWorkloadSummaryShouldNotBeFoundWithin(String username, int seconds) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        ResponseEntity<String> response = testApiClient.getSummary(username, scenarioContext.getAuthorizationHeader());
        scenarioContext.setLastResponse(response);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Then("the event should be routed to the dead letter queue")
    public void theEventShouldBeRoutedToTheDeadLetterQueue() throws JMSException {
        jmsTemplate.setReceiveTimeout(5000);
        Message message = jmsTemplate.receive(deadLetterQueueName);
        assertThat(message)
                .as("A malformed workload event must be routed to the dead letter queue")
                .isNotNull();
        assertThat(message.getStringProperty(REJECTION_REASON_PROPERTY)).isNotBlank();
    }

    private void publishEvent(String username, int minutes, int year, int month, String actionType, String omitField) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("trainerUsername", username);
        fields.put("trainerFirstName", "Cucumber");
        fields.put("trainerLastName", "Trainer");
        fields.put("isActive", true);
        fields.put("trainingDate", LocalDate.of(year, month, 15).toString());
        fields.put("trainingDuration", minutes);
        fields.put("actionType", actionType);
        if (omitField != null) {
            fields.remove(omitField);
        }

        try {
            jmsTemplate.convertAndSend(eventsQueueName, objectMapper.writeValueAsString(fields));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build workload event payload", e);
        }
    }
}