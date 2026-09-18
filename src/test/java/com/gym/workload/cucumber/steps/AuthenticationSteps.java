package com.gym.workload.cucumber.steps;

import com.gym.workload.cucumber.ScenarioContext;
import com.gym.workload.support.TestJwtSupport;
import io.cucumber.java.en.Given;

import java.time.Duration;

/**
 * Sets up the {@code Authorization} header used by subsequent requests in the scenario.
 * Reused by both the REST query feature and the JMS listener feature (the latter still
 * needs a valid caller token to poll the GET endpoint after publishing an event).
 */
public class AuthenticationSteps {

    private final ScenarioContext scenarioContext;

    public AuthenticationSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the request has a valid authentication token")
    public void theRequestHasAValidAuthenticationToken() {
        scenarioContext.setAuthorizationHeader("Bearer " + TestJwtSupport.validToken());
    }

    @Given("the request has no authentication token")
    public void theRequestHasNoAuthenticationToken() {
        scenarioContext.setAuthorizationHeader(null);
    }

    @Given("the request has an invalid authentication token")
    public void theRequestHasAnInvalidAuthenticationToken() {
        scenarioContext.setAuthorizationHeader("Bearer not-a-real-jwt-token");
    }

    @Given("the request has an expired authentication token")
    public void theRequestHasAnExpiredAuthenticationToken() {
        scenarioContext.setAuthorizationHeader("Bearer " + TestJwtSupport.expiredToken());
    }

    @Given("the request is made by a caller that is not on the allowed list")
    public void theRequestIsMadeByACallerThatIsNotOnTheAllowedList() {
        scenarioContext.setAuthorizationHeader(
                "Bearer " + TestJwtSupport.validToken("untrusted-service", Duration.ofMinutes(5)));
    }
}
