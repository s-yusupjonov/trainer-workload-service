package com.gym.workload.service;

import com.gym.workload.dto.request.WorkloadEventRequest;
import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;

public interface TrainerWorkloadService {

    void recordEvent(WorkloadEventRequest request);

    TrainerWorkloadSummaryResponse getSummary(String username);

    MonthWorkloadResponse getMonthSummary(String username, int year, int month);
}