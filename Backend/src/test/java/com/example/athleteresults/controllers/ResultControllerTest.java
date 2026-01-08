package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Result;
import com.example.athleteresults.repositories.AthleteRepository;
import com.example.athleteresults.repositories.ResultRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ResultController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = ResultController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORIES ===== */
    @MockBean private ResultRepository resultRepo;
    @MockBean private AthleteRepository athleteRepo;

    /* =====================================================
       GET /api/results
    ===================================================== */
    @Test
    void getAllResults_shouldReturnList() throws Exception {
        Result r = new Result();
        r.setId(1);
        r.setAthleteId(10);
        r.setRace("100m");

        Mockito.when(resultRepo.findAll()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].race").value("100m"));
    }

    /* =====================================================
       GET /api/results/athlete/{athleteId}
    ===================================================== */
    @Test
    void getResultsByAthlete_shouldReturnList() throws Exception {
        Result r = new Result();
        r.setAthleteId(10);
        r.setRace("200m");

        Mockito.when(resultRepo.findByAthleteId(10)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/results/athlete/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].race").value("200m"));
    }

    /* =====================================================
       GET /api/results/{id}
    ===================================================== */
    @Test
    void getResultById_shouldReturnResult() throws Exception {
        Result r = new Result();
        r.setId(5);
        r.setRace("400m");

        Mockito.when(resultRepo.findById(5)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/api/results/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race").value("400m"));
    }

    @Test
    void getResultById_notFound() throws Exception {
        Mockito.when(resultRepo.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/results/99"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       POST /api/results
    ===================================================== */
    @Test
    void createResult_shouldReturnSaved() throws Exception {
        Result r = new Result();
        r.setAthleteId(10);
        r.setRace("100m");

        Mockito.when(athleteRepo.existsById(10)).thenReturn(true);
        Mockito.when(resultRepo.save(Mockito.any(Result.class))).thenReturn(r);

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race").value("100m"));
    }

    @Test
    void createResult_invalidAthlete_shouldFail() throws Exception {
        Result r = new Result();
        r.setAthleteId(999);

        Mockito.when(athleteRepo.existsById(999)).thenReturn(false);

        mockMvc.perform(post("/api/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    /* =====================================================
       PUT /api/results/{id}
    ===================================================== */
    @Test
    void updateResult_shouldReturnUpdated() throws Exception {
        Result existing = new Result();
        existing.setId(1);
        existing.setAthleteId(10);

        Result updated = new Result();
        updated.setAthleteId(10);
        updated.setRace("Updated Race");

        Mockito.when(athleteRepo.existsById(10)).thenReturn(true);
        Mockito.when(resultRepo.findById(1)).thenReturn(Optional.of(existing));
        Mockito.when(resultRepo.save(Mockito.any(Result.class))).thenReturn(updated);

        mockMvc.perform(put("/api/results/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race").value("Updated Race"));
    }

    /* =====================================================
       DELETE /api/results/{id}
    ===================================================== */
    @Test
    void deleteResult_shouldReturnOk() throws Exception {
        Mockito.when(resultRepo.existsById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/results/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));
    }

    @Test
    void deleteResult_notFound() throws Exception {
        Mockito.when(resultRepo.existsById(5)).thenReturn(false);

        mockMvc.perform(delete("/api/results/5"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       GET /api/results/filter
    ===================================================== */
    @Test
    void filterResults_shouldReturnList() throws Exception {
        Result r = new Result();
        r.setRace("100m");

        Mockito.when(
                resultRepo.findAll(
                        Mockito.<org.springframework.data.jpa.domain.Specification<Result>>any(),
                        Mockito.any(org.springframework.data.domain.Sort.class)
                )
        ).thenReturn(List.of(r));

        mockMvc.perform(get("/api/results/filter")
                        .param("race", "100m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].race").value("100m"));
    }


    /* =====================================================
       GET /api/results/search
    ===================================================== */
    @Test
    void searchResults_shouldReturnList() throws Exception {
        Result r = new Result();
        r.setRace("200m");

        Mockito.when(
                resultRepo.findAll(
                        Mockito.<org.springframework.data.jpa.domain.Specification<Result>>any(),
                        Mockito.any(org.springframework.data.domain.Sort.class)
                )
        ).thenReturn(List.of(r));

        mockMvc.perform(get("/api/results/search")
                        .param("sortBy", "time")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].race").value("200m"));
    }

}
