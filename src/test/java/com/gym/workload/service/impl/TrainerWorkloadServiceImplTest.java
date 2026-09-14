package com.gym.workload.service.impl;

import com.gym.workload.dto.request.ActionType;
import com.gym.workload.dto.request.WorkloadEventRequest;
import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;
import com.gym.workload.exception.InvalidRequestException;
import com.gym.workload.exception.ResourceNotFoundException;
import com.gym.workload.mapper.TrainerWorkloadMapper;
import com.gym.workload.model.TrainerWorkload;
import com.gym.workload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceImplTest {

    @Mock
    private TrainerWorkloadRepository repository;

    @Mock
    private TrainerWorkloadMapper mapper;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    private TrainerWorkload workload;

    @BeforeEach
    void setUp() {
        workload = new TrainerWorkload("trainer.one", "John", "Doe", true);
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
    void recordEvent_addActionAddsMinutesToWorkload() {
        given(repository.getOrCreate("trainer.one", "John", "Doe", true)).willReturn(workload);

        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 60, ActionType.ADD));

        assertThat(workload.getMinutes(2026, 8)).hasValue(60);
    }

    @Test
    void recordEvent_addActionAccumulatesMinutesInSameMonth() {
        given(repository.getOrCreate("trainer.one", "John", "Doe", true)).willReturn(workload);

        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 5), 60, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 20), 30, ActionType.ADD));

        assertThat(workload.getMinutes(2026, 8)).hasValue(90);
    }

    @Test
    void recordEvent_deleteActionSubtractsMinutes() {
        given(repository.getOrCreate("trainer.one", "John", "Doe", true)).willReturn(workload);

        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 90, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 30, ActionType.DELETE));

        assertThat(workload.getMinutes(2026, 8)).hasValue(60);
    }

    @Test
    void recordEvent_deleteNeverGoesBelowZeroAndRemovesEmptyMonth() {
        given(repository.getOrCreate("trainer.one", "John", "Doe", true)).willReturn(workload);

        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 30, ActionType.ADD));
        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 100, ActionType.DELETE));

        assertThat(workload.getMinutes(2026, 8)).isEmpty();
        assertThat(workload.getMinutesByYearMonth()).doesNotContainKey(2026);
    }

    @Test
    void recordEvent_deleteOnUnknownMonthIsNoOp() {
        given(repository.getOrCreate("trainer.one", "John", "Doe", true)).willReturn(workload);

        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 50, ActionType.DELETE));

        assertThat(workload.getMinutes(2026, 8)).isEmpty();
    }

    @Test
    void getSummary_delegatesToMapperWhenTrainerExists() {
        TrainerWorkloadSummaryResponse expected =
                new TrainerWorkloadSummaryResponse("trainer.one", "John", "Doe", true, List.of());
        given(repository.findByUsername("trainer.one")).willReturn(Optional.of(workload));
        given(mapper.toSummaryResponse(workload)).willReturn(expected);

        TrainerWorkloadSummaryResponse actual = service.getSummary("trainer.one");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void getSummary_unknownTrainerThrowsNotFound() {
        given(repository.findByUsername("unknown.trainer")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary("unknown.trainer"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthSummary_delegatesToMapperWhenMinutesExist() {
        given(repository.getOrCreate("trainer.one", "John", "Doe", true)).willReturn(workload);
        service.recordEvent(buildRequest("trainer.one", LocalDate.of(2026, 8, 1), 60, ActionType.ADD));

        MonthWorkloadResponse expected = new MonthWorkloadResponse("trainer.one", 2026, 8, 60);
        given(repository.findByUsername("trainer.one")).willReturn(Optional.of(workload));
        given(mapper.toMonthResponse(workload, 2026, 8, 60)).willReturn(expected);

        MonthWorkloadResponse actual = service.getMonthSummary("trainer.one", 2026, 8);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void getMonthSummary_unknownMonthThrowsNotFound() {
        given(repository.findByUsername("trainer.one")).willReturn(Optional.of(workload));

        assertThatThrownBy(() -> service.getMonthSummary("trainer.one", 2026, 9))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthSummary_invalidMonthThrowsInvalidRequestAndSkipsLookup() {
        assertThatThrownBy(() -> service.getMonthSummary("trainer.one", 2026, 13))
                .isInstanceOf(InvalidRequestException.class);

        verify(repository, never()).findByUsername("trainer.one");
    }
}