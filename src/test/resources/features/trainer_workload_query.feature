Feature: Trainer workload queries
  As an authorized internal caller
  I want to query a trainer's recorded workload
  So that I can build accurate training reports

  Background:
    Given the request has a valid authentication token

  Scenario: Fetching the full summary for a trainer with recorded workload
    Given a workload record exists for trainer "workload.query.trainer1" with 90 minutes recorded in year 2026 month 8
    When the client requests the workload summary for trainer "workload.query.trainer1"
    Then the response status should be 200
    And the response should report 90 minutes for month 8

  Scenario: Fetching the full summary for a trainer with no recorded workload
    When the client requests the workload summary for trainer "workload.query.unknown.trainer"
    Then the response status should be 404

  Scenario: Fetching a specific month for a trainer with recorded workload
    Given a workload record exists for trainer "workload.query.trainer2" with 45 minutes recorded in year 2026 month 9
    When the client requests the workload for trainer "workload.query.trainer2" in year 2026 and month 9
    Then the response status should be 200
    And the response should report 45 minutes for month 9

  Scenario: Fetching a month that has no recorded workload
    Given a workload record exists for trainer "workload.query.trainer3" with 30 minutes recorded in year 2026 month 5
    When the client requests the workload for trainer "workload.query.trainer3" in year 2026 and month 6
    Then the response status should be 404

  Scenario: Requesting a month value outside the valid range
    Given a workload record exists for trainer "workload.query.trainer4" with 20 minutes recorded in year 2026 month 3
    When the client requests the workload for trainer "workload.query.trainer4" in year 2026 and month 13
    Then the response status should be 400

  Scenario: Requesting without an authentication token
    Given the request has no authentication token
    When the client requests the workload summary for trainer "workload.query.trainer1"
    Then the response status should be 401

  Scenario: Requesting with an expired authentication token
    Given the request has an expired authentication token
    When the client requests the workload summary for trainer "workload.query.trainer1"
    Then the response status should be 401

  Scenario: Requesting with an invalid authentication token
    Given the request has an invalid authentication token
    When the client requests the workload summary for trainer "workload.query.trainer1"
    Then the response status should be 401

  Scenario: Requesting from a caller that is not on the allowed list
    Given the request is made by a caller that is not on the allowed list
    When the client requests the workload summary for trainer "workload.query.trainer1"
    Then the response status should be 403
