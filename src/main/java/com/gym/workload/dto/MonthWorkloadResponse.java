package com.gym.workload.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthWorkloadResponse {

    private final String trainerUsername;
    private final int year;
    private final int month;
    private final int trainingSummaryDuration;
}
