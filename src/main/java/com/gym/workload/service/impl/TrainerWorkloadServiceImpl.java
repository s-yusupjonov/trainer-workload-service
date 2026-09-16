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
import com.gym.workload.service.TrainerWorkloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {

    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadServiceImpl.class);

    private final TrainerWorkloadRepository repository;
    private final TrainerWorkloadMapper mapper;

    public TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository, TrainerWorkloadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void recordEvent(WorkloadEventRequest request) {
        Optional<TrainerWorkloadDocument> existing = repository.findByTrainerUsername(request.getTrainerUsername());
        boolean isNewTrainer = existing.isEmpty();
        TrainerWorkloadDocument document = existing.orElseGet(() -> newDocument(request.getTrainerUsername()));

        applyProfile(document, request);

        LocalDate trainingDate = request.getTrainingDate();
        int year = trainingDate.getYear();
        int month = trainingDate.getMonthValue();
        boolean isNewYearMonth = !isNewTrainer && findMonthEntry(document, year, month).isEmpty();

        MonthEntry monthEntry = resolveMonthEntry(document, year, month);

        if (isNewTrainer) {
            monthEntry.setTrainingSummaryDuration(request.getTrainingDuration());
        } else {
            applyDuration(monthEntry, request.getActionType(), request.getTrainingDuration());
        }

        repository.save(document);

        logOutcome(request, year, month, isNewTrainer, isNewYearMonth, monthEntry.getTrainingSummaryDuration());
    }

    @Override
    public TrainerWorkloadSummaryResponse getSummary(String username) {
        TrainerWorkloadDocument document = findDocumentOrThrow(username);
        return mapper.toSummaryResponse(document);
    }

    @Override
    public MonthWorkloadResponse getMonthSummary(String username, int year, int month) {
        if (month < 1 || month > 12) {
            throw new InvalidRequestException("Month must be between 1 and 12");
        }

        TrainerWorkloadDocument document = findDocumentOrThrow(username);

        int trainingSummaryDuration = findMonthEntry(document, year, month)
                .map(MonthEntry::getTrainingSummaryDuration)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No workload recorded for trainer '" + username + "' in " + year + "-" + month));

        return mapper.toMonthResponse(document, year, month, trainingSummaryDuration);
    }

    private TrainerWorkloadDocument findDocumentOrThrow(String username) {
        return repository.findByTrainerUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("No workload recorded for trainer '" + username + "'"));
    }

    private TrainerWorkloadDocument newDocument(String trainerUsername) {
        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername(trainerUsername);
        return document;
    }

    private void applyProfile(TrainerWorkloadDocument document, WorkloadEventRequest request) {
        document.setTrainerFirstName(request.getTrainerFirstName());
        document.setTrainerLastName(request.getTrainerLastName());
        document.setTrainerStatus(request.isActive());
    }

    private MonthEntry resolveMonthEntry(TrainerWorkloadDocument document, int year, int month) {
        YearEntry yearEntry = resolveYearEntry(document, year);
        return resolveMonthEntry(yearEntry, month);
    }

    private YearEntry resolveYearEntry(TrainerWorkloadDocument document, int year) {
        return document.getYears().stream()
                .filter(entry -> entry.getYear() == year)
                .findFirst()
                .orElseGet(() -> {
                    YearEntry created = new YearEntry(year, new ArrayList<>());
                    document.getYears().add(created);
                    return created;
                });
    }

    private MonthEntry resolveMonthEntry(YearEntry yearEntry, int month) {
        return yearEntry.getMonths().stream()
                .filter(entry -> entry.getMonth() == month)
                .findFirst()
                .orElseGet(() -> {
                    MonthEntry created = new MonthEntry(month, 0);
                    yearEntry.getMonths().add(created);
                    return created;
                });
    }

    private Optional<MonthEntry> findMonthEntry(TrainerWorkloadDocument document, int year, int month) {
        return document.getYears().stream()
                .filter(entry -> entry.getYear() == year)
                .findFirst()
                .flatMap(yearEntry -> yearEntry.getMonths().stream()
                        .filter(entry -> entry.getMonth() == month)
                        .findFirst());
    }

    private void applyDuration(MonthEntry monthEntry, ActionType actionType, int duration) {
        int updated = actionType == ActionType.ADD
                ? monthEntry.getTrainingSummaryDuration() + duration
                : Math.max(0, monthEntry.getTrainingSummaryDuration() - duration);
        monthEntry.setTrainingSummaryDuration(updated);
    }

    private void logOutcome(WorkloadEventRequest request, int year, int month, boolean isNewTrainer,
                            boolean isNewYearMonth, int trainingSummaryDuration) {
        String outcome = isNewTrainer ? "createdDocument" : isNewYearMonth ? "createdYearMonth" : "updatedExisting";
        log.info("Recorded workload trainerUsername={} year={} month={} actionType={} outcome={} trainingSummaryDuration={}",
                request.getTrainerUsername(), year, month, request.getActionType(), outcome, trainingSummaryDuration);
    }
}