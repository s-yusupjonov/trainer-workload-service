package com.gym.workload.mapper;

import com.gym.workload.dto.response.MonthSummary;
import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.response.YearSummary;
import com.gym.workload.model.TrainerWorkload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TrainerWorkloadMapper {

    public TrainerWorkloadSummaryResponse toSummaryResponse(TrainerWorkload workload) {
        List<YearSummary> years = workload.snapshotByYear().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new YearSummary(entry.getKey(), toMonthSummaries(entry.getValue())))
                .toList();

        return new TrainerWorkloadSummaryResponse(
                workload.getTrainerUsername(),
                workload.getTrainerFirstName(),
                workload.getTrainerLastName(),
                workload.isActive(),
                years
        );
    }

    public MonthWorkloadResponse toMonthResponse(TrainerWorkload workload, int year, int month, int minutes) {
        return new MonthWorkloadResponse(workload.getTrainerUsername(), year, month, minutes);
    }

    private List<MonthSummary> toMonthSummaries(Map<Integer, Integer> monthsToMinutes) {
        return monthsToMinutes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MonthSummary(entry.getKey(), entry.getValue()))
                .toList();
    }
}