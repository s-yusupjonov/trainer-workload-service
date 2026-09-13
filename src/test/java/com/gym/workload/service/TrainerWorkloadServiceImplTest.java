package com.gym.workload.service;

import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.MonthWorkloadResponse;
import com.gym.workload.dto.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.WorkloadEventRequest;
import com.gym.workload.exception.InvalidRequestException;
import com.gym.workload.exception.ResourceNotFoundException;
import com.gym.workload.mapper.TrainerWorkloadMapper;
import com.gym.workload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerWorkloadServiceImplTest {

    private TrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        service = new TrainerWorkloadServiceImpl(new TrainerWorkloadRepository(), new TrainerWorkloadMapper());
    }

    private WorkloadEventRequest buildRequest(String username, LocalDate date, int duration, ActionType actionType) {
        WorkloadEventRequest request = new WorkloadEventRequest();
        request.setTrainerUsername(username);
        request.setTrainerFirstName("John");
        request.setTrainerLastName("Doe");
        request.setActive(true);
        request.setTrainingDate(date);
        request.setTrainingDuration(duration);
        request.setActionType(actionType);
        return request;
    }

    @Test
    void recordEvent_addsMinutesForNewTrainer() {
        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 60, ActionType.ADD));

        MonthWorkloadResponse response = service.getMonthSummary("trainer.one", 2026, 8);

        assertThat(response.getTrainingSummaryDuration()).isEqualTo(60);
    }

    @Test
    void recordEvent_accumulatesMultipleAddsInSameMonth() {
        service.recordEvent(buildRequest("trainer.two", LocalDate.of(2026, 8, 5), 60, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.two", LocalDate.of(2026, 8, 20), 30, ActionType.ADD));

        MonthWorkloadResponse response = service.getMonthSummary("trainer.two", 2026, 8);

        assertThat(response.getTrainingSummaryDuration()).isEqualTo(90);
    }

    @Test
    void recordEvent_deleteSubtractsMinutes() {
        service.recordEvent(buildRequest("trainer.three", LocalDate.of(2026, 8, 1), 90, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.three", LocalDate.of(2026, 8, 1), 30, ActionType.DELETE));

        MonthWorkloadResponse response = service.getMonthSummary("trainer.three", 2026, 8);

        assertThat(response.getTrainingSummaryDuration()).isEqualTo(60);
    }

    @Test
    void recordEvent_deleteNeverGoesBelowZeroAndRemovesEmptyMonth() {
        service.recordEvent(buildRequest("trainer.four", LocalDate.of(2026, 8, 1), 30, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.four", LocalDate.of(2026, 8, 1), 100, ActionType.DELETE));

        assertThatThrownBy(() -> service.getMonthSummary("trainer.four", 2026, 8))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recordEvent_deleteOnUnknownMonthIsNoOp() {
        service.recordEvent(buildRequest("trainer.eight", LocalDate.of(2026, 8, 1), 50, ActionType.DELETE));

        assertThatThrownBy(() -> service.getMonthSummary("trainer.eight", 2026, 8))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSummary_returnsAllYearsAndMonthsSorted() {
        service.recordEvent(buildRequest("trainer.five", LocalDate.of(2025, 12, 1), 45, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.five", LocalDate.of(2026, 1, 1), 60, ActionType.ADD));

        TrainerWorkloadSummaryResponse summary = service.getSummary("trainer.five");

        assertThat(summary.getYears()).hasSize(2);
        assertThat(summary.getYears().get(0).getYear()).isEqualTo(2025);
        assertThat(summary.getYears().get(1).getYear()).isEqualTo(2026);
    }

    @Test
    void getSummary_unknownTrainerThrowsNotFound() {
        assertThatThrownBy(() -> service.getSummary("unknown.trainer"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthSummary_unknownMonthThrowsNotFound() {
        service.recordEvent(buildRequest("trainer.six", LocalDate.of(2026, 8, 1), 60, ActionType.ADD));

        assertThatThrownBy(() -> service.getMonthSummary("trainer.six", 2026, 9))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthSummary_invalidMonthThrowsInvalidRequest() {
        service.recordEvent(buildRequest("trainer.seven", LocalDate.of(2026, 8, 1), 60, ActionType.ADD));

        assertThatThrownBy(() -> service.getMonthSummary("trainer.seven", 2026, 13))
                .isInstanceOf(InvalidRequestException.class);
    }
}
