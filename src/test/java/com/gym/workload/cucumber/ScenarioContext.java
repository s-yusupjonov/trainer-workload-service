package com.gym.workload.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Holds per-scenario state (current auth header, last HTTP response, current trainer
 * username) so step classes can share context without a single monolithic steps class.
 * Reset automatically between scenarios by cucumber-spring's {@link ScenarioScope}.
 */
@Component
@ScenarioScope
public class ScenarioContext {

    private String authorizationHeader;
    private ResponseEntity<String> lastResponse;
    private String currentTrainerUsername;

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public ResponseEntity<String> getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ResponseEntity<String> lastResponse) {
        this.lastResponse = lastResponse;
    }

    public String getCurrentTrainerUsername() {
        return currentTrainerUsername;
    }

    public void setCurrentTrainerUsername(String currentTrainerUsername) {
        this.currentTrainerUsername = currentTrainerUsername;
    }
}
