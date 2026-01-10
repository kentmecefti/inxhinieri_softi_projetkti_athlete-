package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.WeightMetric;
import com.example.athleteresults.repositories.WeightMetricRepository;
import com.example.athleteresults.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WeightMetricController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = WeightMetricController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class WeightMetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORY ===== */
    @MockBean
    private WeightMetricRepository weightMetricRepository;

    /* =====================================================
       GET /api/metrics/weight?gymId=1
    ===================================================== */
    @Test
    void getWeightMetricsByGym_shouldReturnList() throws Exception {
        WeightMetric metric = new WeightMetric();

        Mockito.when(weightMetricRepository.findByGymSessionId(1))
                .thenReturn(List.of(metric));

        mockMvc.perform(get("/api/metrics/weight")
                        .param("gymId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       POST /api/metrics/weight
    ===================================================== */
    @Test
    void createWeightMetric_shouldReturnCreated() throws Exception {
        WeightMetric metric = new WeightMetric();

        Mockito.when(weightMetricRepository.save(Mockito.any(WeightMetric.class)))
                .thenReturn(metric);

        mockMvc.perform(post("/api/metrics/weight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(metric)))
                .andExpect(status().isCreated());
    }

    /* =====================================================
       DELETE /api/metrics/weight/{id}
    ===================================================== */
    @Test
    void deleteWeightMetric_shouldReturnNoContent() throws Exception {
        Mockito.when(weightMetricRepository.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/metrics/weight/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteWeightMetric_notFound() throws Exception {
        Mockito.when(weightMetricRepository.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/metrics/weight/99"))
                .andExpect(status().isNotFound());
    }
}
