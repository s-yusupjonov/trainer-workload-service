package com.gym.workload.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthSummary {

    private final int month;
    private final int trainingSummaryDuration;
}
