package com.gym.workload.cucumber.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.workload.cucumber.ScenarioContext;
import com.gym.workload.cucumber.TestApiClient;
import com.gym.workload.model.TrainerWorkloadDocument;
import com.gym.workload.model.TrainerWorkloadDocument.MonthEntry;
import com.gym.workload.model.TrainerWorkloadDocument.YearEntry;
import com.gym.workload.repository.TrainerWorkloadRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions covering GET /api/trainer-workloads/{username} and
 * /api/trainer-workloads/{username}/{year}/{month}. Data is seeded directly through the
 * repository (same MongoDB the running server uses) so scenarios stay independent of the
 * JMS listener under test in the sibling feature.
 */
public class TrainerWorkloadQuerySteps {

    private final ScenarioContext scenarioContext;
    private final TestApiClient testApiClient;
    private final TrainerWorkloadRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TrainerWorkloadQuerySteps(ScenarioContext scenarioContext,
                                      TestApiClient testApiClient,
                                      TrainerWorkloadRepository repository) {
        this.scenarioContext = scenarioContext;
        this.testApiClient = testApiClient;
        this.repository = repository;
    }

    @Given("a workload record exists for trainer {string} with {int} minutes recorded in year {int} month {int}")
    public void aWorkloadRecordExists(String username, int minutes, int year, int month) {
        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername(username);
        document.setTrainerFirstName("Cucumber");
        document.setTrainerLastName("Trainer");
        document.setTrainerStatus(true);

        List<MonthEntry> months = new ArrayList<>(List.of(new MonthEntry(month, minutes)));
        document.setYears(new ArrayList<>(List.of(new YearEntry(year, months))));

        repository.save(document);
        scenarioContext.setCurrentTrainerUsername(username);
    }

    @When("the client requests the workload summary for trainer {string}")
    public void theClientRequestsTheWorkloadSummary(String username) {
        scenarioContext.setLastResponse(testApiClient.getSummary(username, scenarioContext.getAuthorizationHeader()));
    }

    @When("the client requests the workload for trainer {string} in year {int} and month {int}")
    public void theClientRequestsTheWorkloadForMonth(String username, int year, int month) {
        scenarioContext.setLastResponse(
                testApiClient.getMonthSummary(username, year, month, scenarioContext.getAuthorizationHeader()));
    }

    @Then("the response should report {int} minutes for month {int}")
    public void theResponseShouldReportMinutesForMonth(int minutes, int month) throws Exception {
        JsonNode body = objectMapper.readTree(scenarioContext.getLastResponse().getBody());

        // MonthWorkloadResponse is flat (year/month/trainingSummaryDuration at top level);
        // TrainerWorkloadSummaryResponse nests months under years. Handle both shapes.
        if (body.has("month") && body.has("trainingSummaryDuration")) {
            assertThat(body.get("month").asInt()).isEqualTo(month);
            assertThat(body.get("trainingSummaryDuration").asInt()).isEqualTo(minutes);
            return;
        }
        assertThat(findMonthDuration(body, month)).isEqualTo(minutes);
    }

    private int findMonthDuration(JsonNode body, int month) {
        for (JsonNode year : body.get("years")) {
            for (JsonNode monthNode : year.get("months")) {
                if (monthNode.get("month").asInt() == month) {
                    return monthNode.get("trainingSummaryDuration").asInt();
                }
            }
        }
        throw new AssertionError("Month " + month + " was not present in response: " + body);
    }
}
