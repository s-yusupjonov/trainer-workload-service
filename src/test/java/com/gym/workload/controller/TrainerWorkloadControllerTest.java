package com.gym.workload.controller;

import com.gym.workload.config.SecurityConfig;
import com.gym.workload.dto.response.MonthSummary;
import com.gym.workload.dto.response.MonthWorkloadResponse;
import com.gym.workload.dto.response.TrainerWorkloadSummaryResponse;
import com.gym.workload.dto.response.YearSummary;
import com.gym.workload.exception.InvalidRequestException;
import com.gym.workload.exception.ResourceNotFoundException;
import com.gym.workload.security.JwtAuthenticationEntryPoint;
import com.gym.workload.security.JwtAuthenticationFilter;
import com.gym.workload.security.JwtTokenValidator;
import com.gym.workload.security.SecurityErrorResponseWriter;
import com.gym.workload.service.TrainerWorkloadService;
import com.gym.workload.support.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainerWorkloadController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenValidator.class,
        SecurityErrorResponseWriter.class, JwtAuthenticationEntryPoint.class})
class TrainerWorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainerWorkloadService trainerWorkloadService;

    private String bearerHeader() {
        return "Bearer " + TestJwtSupport.validToken();
    }

    @Test
    void getSummary_existingTrainerReturnsOk() throws Exception {
        TrainerWorkloadSummaryResponse response = new TrainerWorkloadSummaryResponse(
                "trainer.one", "John", "Doe", true,
                List.of(new YearSummary(2026, List.of(new MonthSummary(8, 60)))));

        given(trainerWorkloadService.getSummary("trainer.one")).willReturn(response);

        mockMvc.perform(get("/api/trainer-workloads/trainer.one")
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("trainer.one"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));
    }

    @Test
    void getSummary_missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/trainer-workloads/trainer.one"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummary_unknownTrainerReturnsNotFound() throws Exception {
        given(trainerWorkloadService.getSummary(anyString()))
                .willThrow(new ResourceNotFoundException("No workload recorded for trainer 'unknown'"));

        mockMvc.perform(get("/api/trainer-workloads/unknown")
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getMonthSummary_existingReturnsOk() throws Exception {
        given(trainerWorkloadService.getMonthSummary("trainer.one", 2026, 8))
                .willReturn(new MonthWorkloadResponse("trainer.one", 2026, 8, 60));

        mockMvc.perform(get("/api/trainer-workloads/trainer.one/2026/8")
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(60));
    }

    @Test
    void getMonthSummary_unknownReturnsNotFound() throws Exception {
        given(trainerWorkloadService.getMonthSummary(anyString(), anyInt(), anyInt()))
                .willThrow(new ResourceNotFoundException("No workload recorded"));

        mockMvc.perform(get("/api/trainer-workloads/trainer.one/2026/9")
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMonthSummary_invalidMonthReturnsBadRequest() throws Exception {
        given(trainerWorkloadService.getMonthSummary(anyString(), anyInt(), anyInt()))
                .willThrow(new InvalidRequestException("Month must be between 1 and 12"));

        mockMvc.perform(get("/api/trainer-workloads/trainer.one/2026/13")
                        .header("Authorization", bearerHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonthSummary_missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/trainer-workloads/trainer.one/2026/8"))
                .andExpect(status().isUnauthorized());
    }
}