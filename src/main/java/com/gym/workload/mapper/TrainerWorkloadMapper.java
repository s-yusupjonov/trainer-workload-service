package com.gym.workload.mapper;

import com.gym.workload.dto.response.MonthSummary;
import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.response.YearSummary;
import com.gym.workload.model.TrainerWorkloadDocument;
import com.gym.workload.model.TrainerWorkloadDocument.MonthEntry;
import com.gym.workload.model.TrainerWorkloadDocument.YearEntry;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class TrainerWorkloadMapper {

    public TrainerWorkloadSummaryResponse toSummaryResponse(TrainerWorkloadDocument document) {
        List<YearSummary> years = document.getYears().stream()
                .sorted(Comparator.comparingInt(YearEntry::getYear))
                .map(entry -> new YearSummary(entry.getYear(), toMonthSummaries(entry.getMonths())))
                .toList();

        return new TrainerWorkloadSummaryResponse(
                document.getTrainerUsername(),
                document.getTrainerFirstName(),
                document.getTrainerLastName(),
                document.getTrainerStatus(),
                years
        );
    }

    public MonthWorkloadResponse toMonthResponse(TrainerWorkloadDocument document, int year, int month,
                                                 int trainingSummaryDuration) {
        return new MonthWorkloadResponse(document.getTrainerUsername(), year, month, trainingSummaryDuration);
    }

    private List<MonthSummary> toMonthSummaries(List<MonthEntry> months) {
        return months.stream()
                .sorted(Comparator.comparingInt(MonthEntry::getMonth))
                .map(entry -> new MonthSummary(entry.getMonth(), entry.getTrainingSummaryDuration()))
                .toList();
    }
}