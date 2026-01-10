package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.PlyoMetric;
import com.example.athleteresults.repositories.PlyoMetricRepository;
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
 * Unit tests for PlyoMetricController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = PlyoMetricController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PlyoMetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlyoMetricRepository plyoMetricRepository;

    /* =====================================================
       GET /api/metrics/plyo?gymId=1
    ===================================================== */
    @Test
    void getPlyoMetricsByGym_shouldReturnList() throws Exception {
        PlyoMetric m = new PlyoMetric();
        m.setHeight(45.5);
        m.setContacts(20);
        m.setIntensity(3);

        Mockito.when(plyoMetricRepository.findByGymSessionId(1))
                .thenReturn(List.of(m));

        mockMvc.perform(get("/api/metrics/plyo")
                        .param("gymId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].height").value(45.5))
                .andExpect(jsonPath("$[0].contacts").value(20))
                .andExpect(jsonPath("$[0].intensity").value(3));
    }

    /* =====================================================
       POST /api/metrics/plyo
    ===================================================== */
    @Test
    void createPlyoMetric_shouldReturnCreated() throws Exception {
        PlyoMetric m = new PlyoMetric();
        m.setHeight(50.0);
        m.setContacts(25);
        m.setIntensity(4);

        Mockito.when(plyoMetricRepository.save(Mockito.any(PlyoMetric.class)))
                .thenReturn(m);

        mockMvc.perform(post("/api/metrics/plyo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.height").value(50.0))
                .andExpect(jsonPath("$.contacts").value(25))
                .andExpect(jsonPath("$.intensity").value(4));
    }

    /* =====================================================
       DELETE /api/metrics/plyo/{id}
    ===================================================== */
    @Test
    void deletePlyoMetric_shouldReturnNoContent() throws Exception {
        Mockito.when(plyoMetricRepository.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/metrics/plyo/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePlyoMetric_notFound() throws Exception {
        Mockito.when(plyoMetricRepository.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/metrics/plyo/99"))
                .andExpect(status().isNotFound());
    }
}
