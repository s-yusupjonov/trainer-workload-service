package com.gym.workload.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class YearSummary {

    private final int year;
    private final List<MonthSummary> months;
}
