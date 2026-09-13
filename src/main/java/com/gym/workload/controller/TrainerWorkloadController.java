package com.gym.workload.controller;

import com.gym.workload.dto.ErrorResponse;
import com.gym.workload.dto.MonthWorkloadResponse;
import com.gym.workload.dto.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.WorkloadEventRequest;
import com.gym.workload.service.TrainerWorkloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainer-workloads")
@Tag(name = "Trainer Workloads", description = "Operations for recording and retrieving trainer workload data")
public class TrainerWorkloadController {

    private final TrainerWorkloadService trainerWorkloadService;

    public TrainerWorkloadController(TrainerWorkloadService trainerWorkloadService) {
        this.trainerWorkloadService = trainerWorkloadService;
    }

    @Operation(summary = "Record a trainer workload event",
            description = "Adds or removes training minutes for a trainer in a given month")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller not permitted",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Void> recordEvent(@Valid @RequestBody WorkloadEventRequest request) {
        trainerWorkloadService.recordEvent(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get the full workload summary for a trainer",
            description = "Returns training minutes grouped by year and month")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary found",
                    content = @Content(schema = @Schema(implementation = TrainerWorkloadSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Trainer has no recorded workload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}")
    public ResponseEntity<TrainerWorkloadSummaryResponse> getSummary(@PathVariable String username) {
        return ResponseEntity.ok(trainerWorkloadService.getSummary(username));
    }

    @Operation(summary = "Get the workload summary for a trainer in a specific month")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Month summary found",
                    content = @Content(schema = @Schema(implementation = MonthWorkloadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid month value",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No workload recorded for that month",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{username}/{year}/{month}")
    public ResponseEntity<MonthWorkloadResponse> getMonthSummary(@PathVariable String username,
                                                                  @PathVariable int year,
                                                                  @PathVariable int month) {
        return ResponseEntity.ok(trainerWorkloadService.getMonthSummary(username, year, month));
    }
}
