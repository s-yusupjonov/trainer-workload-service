package com.gym.workload.service;

import com.gym.workload.domain.TrainerWorkload;
import com.gym.workload.dto.ActionType;
import com.gym.workload.dto.MonthWorkloadResponse;
import com.gym.workload.dto.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.WorkloadEventRequest;
import com.gym.workload.exception.InvalidRequestException;
import com.gym.workload.exception.ResourceNotFoundException;
import com.gym.workload.mapper.TrainerWorkloadMapper;
import com.gym.workload.repository.TrainerWorkloadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.OptionalInt;

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
            workload.addMinutes(year, month, request.getTrainingDuration());
        } else {
            workload.subtractMinutes(year, month, request.getTrainingDuration());
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
}
