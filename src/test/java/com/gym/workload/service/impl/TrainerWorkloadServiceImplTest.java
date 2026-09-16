package com.gym.workload.service.impl;

import com.gym.workload.dto.request.ActionType;
import com.gym.workload.dto.request.WorkloadEventRequest;
import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;
import com.gym.workload.exception.InvalidRequestException;
import com.gym.workload.exception.ResourceNotFoundException;
import com.gym.workload.mapper.TrainerWorkloadMapper;
import com.gym.workload.model.TrainerWorkloadDocument;
import com.gym.workload.model.TrainerWorkloadDocument.MonthEntry;
import com.gym.workload.model.TrainerWorkloadDocument.YearEntry;
import com.gym.workload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceImplTest {

    private static final String USERNAME = "trainer.one";

    @Mock
    private TrainerWorkloadRepository repository;

    @Mock
    private TrainerWorkloadMapper mapper;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    private WorkloadEventRequest buildRequest(LocalDate date, int duration, ActionType actionType) {
        WorkloadEventRequest request = new WorkloadEventRequest();
        request.setTrainerUsername(USERNAME);
        request.setTrainerFirstName("John");
        request.setTrainerLastName("Doe");
        request.setActive(true);
        request.setTrainingDate(date);
        request.setTrainingDuration(duration);
        request.setActionType(actionType);
        return request;
    }

    private TrainerWorkloadDocument existingDocumentWithMonth(int year, int month, int duration) {
        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername(USERNAME);
        document.setTrainerFirstName("John");
        document.setTrainerLastName("Doe");
        document.setTrainerStatus(true);
        document.setYears(new ArrayList<>(List.of(
                new YearEntry(year, new ArrayList<>(List.of(new MonthEntry(month, duration))))
        )));
        return document;
    }

    @Test
    void recordEvent_firstEventForUnknownTrainerCreatesDocumentWithGivenDuration() {
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.empty());

        service.recordEvent(buildRequest(LocalDate.of(2026, 8, 1), 60, ActionType.ADD));

        ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
        verify(repository).save(captor.capture());
        TrainerWorkloadDocument saved = captor.getValue();

        assertThat(saved.getTrainerUsername())
                .as("Newly created document should carry the trainerUsername from the event")
                .isEqualTo(USERNAME);
        assertThat(saved.getTrainerFirstName())
                .as("Newly created document should carry the trainerFirstName from the event")
                .isEqualTo("John");
        assertThat(saved.getTrainerStatus())
                .as("Newly created document should carry the isActive flag from the event")
                .isTrue();
        assertThat(saved.getYears())
                .as("Newly created document should contain exactly one year entry")
                .hasSize(1);
        assertThat(saved.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("First-ever event should seed the month duration with the event's duration")
                .isEqualTo(60);
    }

    @Test
    void recordEvent_secondAddEventForExistingYearMonthIncrementsDuration() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 60);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));

        service.recordEvent(buildRequest(LocalDate.of(2026, 8, 20), 30, ActionType.ADD));

        assertThat(existing.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("A second ADD event in the same year/month should accumulate onto the existing duration")
                .isEqualTo(90);
    }

    @Test
    void recordEvent_eventForNewYearMonthOnExistingTrainerAddsSeparateMonthEntry() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 60);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));

        service.recordEvent(buildRequest(LocalDate.of(2026, 9, 1), 45, ActionType.ADD));

        assertThat(existing.getYears().get(0).getMonths())
                .as("A new month within an existing year should be appended, not replace the existing month")
                .hasSize(2);
        assertThat(existing.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("The pre-existing month's duration must remain untouched")
                .isEqualTo(60);
        assertThat(existing.getYears().get(0).getMonths().get(1).getTrainingSummaryDuration())
                .as("The newly added month should hold the new event's duration")
                .isEqualTo(45);
    }

    @Test
    void recordEvent_eventForNewYearOnExistingTrainerAddsSeparateYearEntry() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 60);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));

        service.recordEvent(buildRequest(LocalDate.of(2027, 1, 1), 20, ActionType.ADD));

        assertThat(existing.getYears())
                .as("A training date in a new year should append a new year entry")
                .hasSize(2);
        assertThat(existing.getYears().get(1).getYear())
                .as("The newly added year entry should carry the new year value")
                .isEqualTo(2027);
    }

    @Test
    void recordEvent_deleteActionSubtractsDurationFromExistingMonth() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 90);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));

        service.recordEvent(buildRequest(LocalDate.of(2026, 8, 1), 30, ActionType.DELETE));

        assertThat(existing.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("A DELETE event should subtract its duration from the existing month total")
                .isEqualTo(60);
    }

    @Test
    void recordEvent_deleteActionFloorsAtZeroWhenSubtractingMoreThanRecorded() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 30);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));

        service.recordEvent(buildRequest(LocalDate.of(2026, 8, 1), 100, ActionType.DELETE));

        assertThat(existing.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("DELETE must never drive the recorded duration below zero")
                .isZero();
    }

    @Test
    void getSummary_delegatesToMapperWhenTrainerExists() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 60);
        TrainerWorkloadSummaryResponse expected =
                new TrainerWorkloadSummaryResponse(USERNAME, "John", "Doe", true, List.of());
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));
        given(mapper.toSummaryResponse(existing)).willReturn(expected);

        TrainerWorkloadSummaryResponse actual = service.getSummary(USERNAME);

        assertThat(actual)
                .as("getSummary should return exactly what the mapper produces for the found document")
                .isSameAs(expected);
    }

    @Test
    void getSummary_unknownUsernameThrowsResourceNotFoundException() {
        given(repository.findByTrainerUsername("unknown.trainer")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary("unknown.trainer"))
                .as("getSummary for a username with no stored document should throw ResourceNotFoundException")
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthSummary_delegatesToMapperWhenMonthEntryExists() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 60);
        MonthWorkloadResponse expected = new MonthWorkloadResponse(USERNAME, 2026, 8, 60);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));
        given(mapper.toMonthResponse(existing, 2026, 8, 60)).willReturn(expected);

        MonthWorkloadResponse actual = service.getMonthSummary(USERNAME, 2026, 8);

        assertThat(actual)
                .as("getMonthSummary should return exactly what the mapper produces for the found month entry")
                .isSameAs(expected);
    }

    @Test
    void getMonthSummary_outOfRangeMonthThrowsInvalidRequestExceptionWithoutRepositoryLookup() {
        assertThatThrownBy(() -> service.getMonthSummary(USERNAME, 2026, 13))
                .as("A month outside 1-12 should be rejected before any repository lookup")
                .isInstanceOf(InvalidRequestException.class);

        verify(repository, never()).findByTrainerUsername(USERNAME);
    }

    @Test
    void getMonthSummary_zeroMonthThrowsInvalidRequestException() {
        assertThatThrownBy(() -> service.getMonthSummary(USERNAME, 2026, 0))
                .as("Month 0 is out of the valid 1-12 range and must be rejected")
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getMonthSummary_unknownUsernameThrowsResourceNotFoundException() {
        given(repository.findByTrainerUsername("unknown.trainer")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMonthSummary("unknown.trainer", 2026, 8))
                .as("getMonthSummary for a username with no stored document should throw ResourceNotFoundException")
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMonthSummary_yearMonthWithNoRecordedEntryThrowsResourceNotFoundException() {
        TrainerWorkloadDocument existing = existingDocumentWithMonth(2026, 8, 60);
        given(repository.findByTrainerUsername(USERNAME)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getMonthSummary(USERNAME, 2026, 9))
                .as("A trainer with no entry for the requested year/month should raise ResourceNotFoundException")
                .isInstanceOf(ResourceNotFoundException.class);
    }
}