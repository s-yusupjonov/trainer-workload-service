package com.gym.workload.cucumber.steps;

import com.gym.workload.cucumber.ScenarioContext;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic assertions shared across every feature file, following gym-crm's convention
 * of keeping cross-cutting steps (like a plain status-code check) in one place.
 */
public class CommonSteps {

    private final ScenarioContext scenarioContext;

    public CommonSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertThat(scenarioContext.getLastResponse())
                .as("A response must have been captured before asserting its status code")
                .isNotNull();
        assertThat(scenarioContext.getLastResponse().getStatusCode().value()).isEqualTo(expectedStatus);
    }
}
