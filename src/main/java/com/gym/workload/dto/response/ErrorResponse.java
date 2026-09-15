package com.gym.workload.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String transactionId;
}
