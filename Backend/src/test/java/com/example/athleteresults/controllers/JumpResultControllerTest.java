package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.JumpResult;
import com.example.athleteresults.repositories.AthleteRepository;
import com.example.athleteresults.repositories.JumpResultRepository;
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
 * Unit tests for JumpResultController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = JumpResultController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class JumpResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JumpResultRepository repo;

    @MockBean
    private AthleteRepository athleteRepo;

    /* =====================================================
       GET /api/jumpresults
    ===================================================== */
    @Test
    void getAllJumpResults_shouldReturnList() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setJumpId(1);
        jr.setJumpType("Long Jump");

        Mockito.when(repo.findAll()).thenReturn(List.of(jr));

        mockMvc.perform(get("/api/jumpresults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jumpType").value("Long Jump"));
    }

    /* =====================================================
       GET /api/jumpresults/athlete/{athleteId}
    ===================================================== */
    @Test
    void getJumpResultsByAthlete_shouldReturnList() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setAthleteId(10);
        jr.setJumpType("Triple Jump");

        Mockito.when(repo.findByAthleteId(10)).thenReturn(List.of(jr));

        mockMvc.perform(get("/api/jumpresults/athlete/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jumpType").value("Triple Jump"));
    }

    /* =====================================================
       GET /api/jumpresults/{id}
    ===================================================== */
    @Test
    void getJumpResultById_shouldReturnResult() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setJumpId(5);
        jr.setJumpType("High Jump");

        Mockito.when(repo.findById(5)).thenReturn(Optional.of(jr));

        mockMvc.perform(get("/api/jumpresults/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jumpType").value("High Jump"));
    }

    @Test
    void getJumpResultById_notFound() throws Exception {
        Mockito.when(repo.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jumpresults/99"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       POST /api/jumpresults
    ===================================================== */
    @Test
    void createJumpResult_shouldReturnSaved() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setAthleteId(3);
        jr.setJumpDate(LocalDate.now());
        jr.setJumpType("Long Jump");
        jr.setDistanceM(6.55);
        jr.setNotes("Good takeoff");

        Mockito.when(athleteRepo.existsById(3)).thenReturn(true);
        Mockito.when(repo.save(Mockito.any(JumpResult.class))).thenReturn(jr);

        mockMvc.perform(post("/api/jumpresults")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jumpType").value("Long Jump"));
    }

    @Test
    void createJumpResult_invalidAthlete_shouldFail() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setAthleteId(999);

        Mockito.when(athleteRepo.existsById(999)).thenReturn(false);

        mockMvc.perform(post("/api/jumpresults")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jr)))
                .andExpect(status().isBadRequest());
    }

    /* =====================================================
       PUT /api/jumpresults/{id}
    ===================================================== */
    @Test
    void updateJumpResult_shouldReturnUpdated() throws Exception {
        JumpResult existing = new JumpResult();
        existing.setJumpId(1);
        existing.setAthleteId(5);

        JumpResult updated = new JumpResult();
        updated.setAthleteId(5);
        updated.setJumpType("Updated Jump");
        updated.setDistanceM(7.25);
        updated.setNotes("Strong wind");

        Mockito.when(repo.findById(1)).thenReturn(Optional.of(existing));
        Mockito.when(repo.save(Mockito.any(JumpResult.class))).thenReturn(updated);

        mockMvc.perform(put("/api/jumpresults/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jumpType").value("Updated Jump"))
                .andExpect(jsonPath("$.notes").value("Strong wind"));
    }

    @Test
    void updateJumpResult_notFound() throws Exception {
        Mockito.when(repo.findById(77)).thenReturn(Optional.empty());

        JumpResult jr = new JumpResult();

        mockMvc.perform(put("/api/jumpresults/77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jr)))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       DELETE /api/jumpresults/{id}
    ===================================================== */
    @Test
    void deleteJumpResult_shouldReturnOk() throws Exception {
        Mockito.when(repo.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/jumpresults/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("iku"));
    }

    /* =====================================================
       GET /api/jumpresults/filter
    ===================================================== */
    @Test
    void filterJumpResults_shouldReturnList() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setJumpType("Long Jump");

        Mockito.when(
                repo.findAll(
                        Mockito.<Specification<JumpResult>>any(),
                        Mockito.any(Sort.class)
                )
        ).thenReturn(List.of(jr));

        mockMvc.perform(get("/api/jumpresults/filter")
                        .param("jumpType", "long"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jumpType").value("Long Jump"));
    }

    /* =====================================================
       GET /api/jumpresults/search
    ===================================================== */
    @Test
    void searchJumpResults_shouldReturnList() throws Exception {
        JumpResult jr = new JumpResult();
        jr.setJumpType("High Jump");

        Mockito.when(
                repo.findAll(
                        Mockito.<Specification<JumpResult>>any(),
                        Mockito.any(Sort.class)
                )
        ).thenReturn(List.of(jr));

        mockMvc.perform(get("/api/jumpresults/search")
                        .param("sortBy", "distancem")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jumpType").value("High Jump"));
    }
}
