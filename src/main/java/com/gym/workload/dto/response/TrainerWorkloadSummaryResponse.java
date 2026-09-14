package com.gym.workload.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TrainerWorkloadSummaryResponse {

    private final String trainerUsername;
    private final String trainerFirstName;
    private final String trainerLastName;

    @JsonProperty("isActive")
    private final boolean active;

    private final List<YearSummary> years;
}
