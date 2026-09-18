package com.gym.workload.cucumber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class TestApiClient {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    public ResponseEntity<String> getSummary(String username, String authorizationHeader) {
        return get("/api/trainer-workloads/" + username, authorizationHeader);
    }

    public ResponseEntity<String> getMonthSummary(String username, int year, int month, String authorizationHeader) {
        return get("/api/trainer-workloads/" + username + "/" + year + "/" + month, authorizationHeader);
    }

    private ResponseEntity<String> get(String path, String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(baseUrl() + path, HttpMethod.GET, entity, String.class);
    }

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }
}