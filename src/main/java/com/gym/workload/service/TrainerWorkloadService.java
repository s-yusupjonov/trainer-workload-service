package com.gym.workload.service;

import com.gym.workload.dto.MonthWorkloadResponse;
import com.gym.workload.dto.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.WorkloadEventRequest;

public interface TrainerWorkloadService {

    void recordEvent(WorkloadEventRequest request);

    TrainerWorkloadSummaryResponse getSummary(String username);

    MonthWorkloadResponse getMonthSummary(String username, int year, int month);
}
