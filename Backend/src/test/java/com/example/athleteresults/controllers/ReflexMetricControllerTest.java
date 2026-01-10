package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.ReflexMetric;
import com.example.athleteresults.repositories.ReflexMetricRepository;
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
 * Unit tests for ReflexMetricController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = ReflexMetricController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ReflexMetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORY ===== */
    @MockBean
    private ReflexMetricRepository reflexMetricRepository;

    /* =====================================================
       GET /api/metrics/reflex?gymId=1
    ===================================================== */
    @Test
    void getReflexMetricsByGym_shouldReturnList() throws Exception {
        ReflexMetric metric = new ReflexMetric();

        Mockito.when(reflexMetricRepository.findByGymSessionId(1))
                .thenReturn(List.of(metric));

        mockMvc.perform(get("/api/metrics/reflex")
                        .param("gymId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       POST /api/metrics/reflex
    ===================================================== */
    @Test
    void createReflexMetric_shouldReturnCreated() throws Exception {
        ReflexMetric metric = new ReflexMetric();

        Mockito.when(reflexMetricRepository.save(Mockito.any(ReflexMetric.class)))
                .thenReturn(metric);

        mockMvc.perform(post("/api/metrics/reflex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(metric)))
                .andExpect(status().isCreated());
    }

    /* =====================================================
       DELETE /api/metrics/reflex/{id}
    ===================================================== */
    @Test
    void deleteReflexMetric_shouldReturnNoContent() throws Exception {
        Mockito.when(reflexMetricRepository.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/metrics/reflex/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReflexMetric_notFound() throws Exception {
        Mockito.when(reflexMetricRepository.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/metrics/reflex/99"))
                .andExpect(status().isNotFound());
    }
}
