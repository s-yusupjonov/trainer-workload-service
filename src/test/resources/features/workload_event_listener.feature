Feature: Workload event listener
  As the trainer workload service
  I want to consume workload events from the message queue
  So that trainer workload summaries stay in sync with the source system

  Background:
    Given the request has a valid authentication token

  Scenario: A valid ADD event increases the trainer's recorded minutes
    When a valid ADD workload event is published for trainer "workload.jms.trainer1" with 40 minutes for year 2026 month 8
    Then the workload summary for trainer "workload.jms.trainer1" should eventually report 40 minutes for year 2026 month 8

  Scenario: A valid DELETE event decreases previously recorded minutes
    Given a workload record exists for trainer "workload.jms.trainer2" with 60 minutes recorded in year 2026 month 8
    When a DELETE workload event is published for trainer "workload.jms.trainer2" with 25 minutes for year 2026 month 8
    Then the workload summary for trainer "workload.jms.trainer2" should eventually report 35 minutes for year 2026 month 8

  Scenario: A malformed event is routed to the dead letter queue instead of being recorded
    When a malformed workload event missing the "trainerUsername" field is published for trainer "workload.jms.trainer3"
    Then the workload summary for trainer "workload.jms.trainer3" should not be found within 3 seconds
    And the event should be routed to the dead letter queue
