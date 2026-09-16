package com.gym.workload.mapper;

import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.response.YearSummary;
import com.gym.workload.model.TrainerWorkloadDocument;
import com.gym.workload.model.TrainerWorkloadDocument.MonthEntry;
import com.gym.workload.model.TrainerWorkloadDocument.YearEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerWorkloadMapperTest {

    private final TrainerWorkloadMapper mapper = new TrainerWorkloadMapper();

    private TrainerWorkloadDocument document(List<YearEntry> years) {
        TrainerWorkloadDocument document = new TrainerWorkloadDocument();
        document.setTrainerUsername("trainer.one");
        document.setTrainerFirstName("John");
        document.setTrainerLastName("Doe");
        document.setTrainerStatus(true);
        document.setYears(new ArrayList<>(years));
        return document;
    }

    @Test
    void toSummaryResponse_copiesTrainerProfileFields() {
        TrainerWorkloadDocument document = document(List.of());

        TrainerWorkloadSummaryResponse response = mapper.toSummaryResponse(document);

        assertThat(response.getTrainerUsername())
                .as("Username must be carried over unchanged")
                .isEqualTo("trainer.one");
        assertThat(response.getTrainerFirstName())
                .as("First name must be carried over unchanged")
                .isEqualTo("John");
        assertThat(response.getTrainerLastName())
                .as("Last name must be carried over unchanged")
                .isEqualTo("Doe");
        assertThat(response.isActive())
                .as("Trainer status must be carried over unchanged")
                .isTrue();
    }

    @Test
    void toSummaryResponse_withNoYears_returnsEmptyYearsList() {
        TrainerWorkloadDocument document = document(List.of());

        TrainerWorkloadSummaryResponse response = mapper.toSummaryResponse(document);

        assertThat(response.getYears())
                .as("A trainer with no recorded years should map to an empty years list, not null")
                .isEmpty();
    }

    @Test
    void toSummaryResponse_sortsYearsAscendingRegardlessOfInputOrder() {
        TrainerWorkloadDocument document = document(List.of(
                new YearEntry(2027, new ArrayList<>()),
                new YearEntry(2025, new ArrayList<>()),
                new YearEntry(2026, new ArrayList<>())
        ));

        TrainerWorkloadSummaryResponse response = mapper.toSummaryResponse(document);

        assertThat(response.getYears())
                .as("Years should be sorted ascending regardless of the order stored in the document")
                .extracting(YearSummary::getYear)
                .containsExactly(2025, 2026, 2027);
    }

    @Test
    void toSummaryResponse_sortsMonthsAscendingWithinEachYear() {
        List<MonthEntry> unsortedMonths = new ArrayList<>(List.of(
                new MonthEntry(9, 30),
                new MonthEntry(1, 60),
                new MonthEntry(5, 45)
        ));
        TrainerWorkloadDocument document = document(List.of(new YearEntry(2026, unsortedMonths)));

        TrainerWorkloadSummaryResponse response = mapper.toSummaryResponse(document);

        assertThat(response.getYears().get(0).getMonths())
                .as("Months within a year should be sorted ascending regardless of storage order")
                .extracting(month -> month.getMonth())
                .containsExactly(1, 5, 9);
    }

    @Test
    void toSummaryResponse_preservesTrainingSummaryDurationPerMonth() {
        TrainerWorkloadDocument document = document(List.of(
                new YearEntry(2026, new ArrayList<>(List.of(new MonthEntry(8, 90))))
        ));

        TrainerWorkloadSummaryResponse response = mapper.toSummaryResponse(document);

        assertThat(response.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration())
                .as("The training summary duration for a month must be mapped without modification")
                .isEqualTo(90);
    }

    @Test
    void toMonthResponse_buildsResponseFromGivenCoordinatesAndDuration() {
        TrainerWorkloadDocument document = document(List.of());

        MonthWorkloadResponse response = mapper.toMonthResponse(document, 2026, 8, 75);

        assertThat(response.getTrainerUsername())
                .as("Month response should carry the document's trainerUsername")
                .isEqualTo("trainer.one");
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(8);
        assertThat(response.getTrainingSummaryDuration())
                .as("Month response should carry the duration value passed in explicitly, not looked up again")
                .isEqualTo(75);
    }
}