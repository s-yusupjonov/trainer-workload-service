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
import com.gym.workload.service.TrainerWorkloadService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {

    private final TrainerWorkloadRepository repository;
    private final TrainerWorkloadMapper mapper;

    public TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository, TrainerWorkloadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void recordEvent(WorkloadEventRequest request) {
        TrainerWorkload workload = repository.getOrCreate(
                request.getTrainerUsername(),
                request.getTrainerFirstName(),
                request.getTrainerLastName(),
                request.isActive()
        );

        LocalDate trainingDate = request.getTrainingDate();
        int year = trainingDate.getYear();
        int month = trainingDate.getMonthValue();

        if (request.getActionType() == ActionType.ADD) {
            addMinutes(workload, year, month, request.getTrainingDuration());
        } else {
            subtractMinutes(workload, year, month, request.getTrainingDuration());
        }
    }

    @Override
    public TrainerWorkloadSummaryResponse getSummary(String username) {
        TrainerWorkload workload = repository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("No workload recorded for trainer '" + username + "'"));
        return mapper.toSummaryResponse(workload);
    }

    @Override
    public MonthWorkloadResponse getMonthSummary(String username, int year, int month) {
        if (month < 1 || month > 12) {
            throw new InvalidRequestException("Month must be between 1 and 12");
        }

        TrainerWorkload workload = repository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("No workload recorded for trainer '" + username + "'"));

        OptionalInt minutes = workload.getMinutes(year, month);
        if (minutes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No workload recorded for trainer '" + username + "' in " + year + "-" + month);
        }

        return mapper.toMonthResponse(workload, year, month, minutes.getAsInt());
    }

    private void addMinutes(TrainerWorkload workload, int year, int month, int minutes) {
        workload.getMinutesByYearMonth()
                .computeIfAbsent(year, key -> new ConcurrentHashMap<>())
                .merge(month, minutes, Integer::sum);
    }

    private void subtractMinutes(TrainerWorkload workload, int year, int month, int minutes) {
        ConcurrentMap<Integer, Integer> months = workload.getMinutesByYearMonth().get(year);
        if (months == null) {
            return;
        }

        months.compute(month, (key, current) -> {
            if (current == null) {
                return null;
            }
            int updated = Math.max(0, current - minutes);
            return updated == 0 ? null : updated;
        });

        if (months.isEmpty()) {
            workload.getMinutesByYearMonth().remove(year, months);
        }
    }
}