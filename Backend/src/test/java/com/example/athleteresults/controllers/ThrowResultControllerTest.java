package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.ThrowResult;
import com.example.athleteresults.entities.Athlete;
import com.example.athleteresults.repositories.ThrowResultRepository;
import com.example.athleteresults.repositories.AthleteRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ThrowResultController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = ThrowResultController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ThrowResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORIES ===== */
    @MockBean
    private ThrowResultRepository repo;

    @MockBean
    private AthleteRepository athleteRepo;

    /* =====================================================
       GET /api/throwresults
    ===================================================== */
    @Test
    void getAllThrowResults_shouldReturnList() throws Exception {
        ThrowResult r = new ThrowResult();

        Mockito.when(repo.findAll()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/throwresults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       GET /api/throwresults/athlete/{athleteId}
    ===================================================== */
    @Test
    void getThrowResultsByAthlete_shouldReturnList() throws Exception {
        ThrowResult r = new ThrowResult();

        Mockito.when(repo.findByAthleteId(10)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/throwresults/athlete/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       GET /api/throwresults/{id}
    ===================================================== */
    @Test
    void getThrowResultById_shouldReturnResult() throws Exception {
        ThrowResult r = new ThrowResult();

        Mockito.when(repo.findById(5)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/api/throwresults/5"))
                .andExpect(status().isOk());
    }

    @Test
    void getThrowResultById_notFound() throws Exception {
        Mockito.when(repo.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/throwresults/99"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       POST /api/throwresults
    ===================================================== */
    @Test
    void createThrowResult_shouldReturnSaved() throws Exception {
        Athlete athlete = new Athlete();
        athlete.setId(1);

        ThrowResult r = new ThrowResult();
        r.setAthlete(athlete);

        Mockito.when(athleteRepo.existsById(1)).thenReturn(true);
        Mockito.when(repo.save(Mockito.any(ThrowResult.class))).thenReturn(r);

        mockMvc.perform(post("/api/throwresults")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isOk());
    }

    @Test
    void createThrowResult_invalidAthlete_shouldFail() throws Exception {
        ThrowResult r = new ThrowResult();

        mockMvc.perform(post("/api/throwresults")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    /* =====================================================
       PUT /api/throwresults/{id}
    ===================================================== */
    @Test
    void updateThrowResult_shouldReturnUpdated() throws Exception {
        Athlete athlete = new Athlete();
        athlete.setId(1);

        ThrowResult existing = new ThrowResult();
        existing.setAthlete(athlete);

        ThrowResult updated = new ThrowResult();
        updated.setAthlete(athlete);

        Mockito.when(athleteRepo.existsById(1)).thenReturn(true);
        Mockito.when(repo.findById(1)).thenReturn(Optional.of(existing));
        Mockito.when(repo.save(Mockito.any(ThrowResult.class))).thenReturn(updated);

        mockMvc.perform(put("/api/throwresults/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk());
    }

    @Test
    void updateThrowResult_notFound() throws Exception {
        Athlete athlete = new Athlete();
        athlete.setId(1);

        ThrowResult updated = new ThrowResult();
        updated.setAthlete(athlete);

        Mockito.when(athleteRepo.existsById(1)).thenReturn(true);
        Mockito.when(repo.findById(77)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/throwresults/77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       DELETE /api/throwresults/{id}
    ===================================================== */
    @Test
    void deleteThrowResult_shouldReturnOk() throws Exception {
        Mockito.when(repo.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/throwresults/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("tralalala"));
    }

    @Test
    void deleteThrowResult_notFound() throws Exception {
        Mockito.when(repo.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/throwresults/99"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       GET /api/throwresults/filter
    ===================================================== */
    @Test
    void filterThrowResults_shouldReturnList() throws Exception {
        ThrowResult r = new ThrowResult();

        Mockito.when(
                repo.findAll(
                        Mockito.<Specification<ThrowResult>>any(),
                        Mockito.any(Sort.class)
                )
        ).thenReturn(List.of(r));

        mockMvc.perform(get("/api/throwresults/filter")
                        .param("throwType", "shot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       GET /api/throwresults/search
    ===================================================== */
    @Test
    void searchThrowResults_shouldReturnList() throws Exception {
        ThrowResult r = new ThrowResult();

        Mockito.when(
                repo.findAll(
                        Mockito.<Specification<ThrowResult>>any(),
                        Mockito.any(Sort.class)
                )
        ).thenReturn(List.of(r));

        mockMvc.perform(get("/api/throwresults/search")
                        .param("sortBy", "distance")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
