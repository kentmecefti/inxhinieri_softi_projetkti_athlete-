package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Athlete;
import com.example.athleteresults.entities.GymSession;
import com.example.athleteresults.repositories.AthleteRepository;
import com.example.athleteresults.repositories.GymSessionRepository;
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
 * Unit tests for GymSessionController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = GymSessionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class GymSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORIES ===== */
    @MockBean private GymSessionRepository gymSessionRepo;
    @MockBean private AthleteRepository athleteRepo;

    /* =====================================================
       GET /api/gymsessions
    ===================================================== */
    @Test
    void getAllGymSessions_shouldReturnList() throws Exception {
        GymSession s = new GymSession();
        s.setId(1);
        s.setExerciseName("Bench Press");

        Mockito.when(gymSessionRepo.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/gymsessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].exerciseName").value("Bench Press"));
    }

    /* =====================================================
       GET /api/gymsessions/athlete/{athleteId}
    ===================================================== */
    @Test
    void getGymSessionsByAthlete_shouldReturnList() throws Exception {
        GymSession s = new GymSession();
        s.setExerciseName("Squat");

        Mockito.when(gymSessionRepo.findByAthleteId(5)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/gymsessions/athlete/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exerciseName").value("Squat"));
    }

    /* =====================================================
       POST /api/gymsessions
    ===================================================== */
    @Test
    void createGymSession_shouldReturnCreated() throws Exception {
        Athlete a = new Athlete();
        a.setId(3);

        GymSession s = new GymSession();
        s.setExerciseName("Deadlift");
        s.setCategory("Strength");
        s.setSessionDate(LocalDate.now());
        s.setAthlete(a);

        Mockito.when(athleteRepo.findById(3)).thenReturn(Optional.of(a));
        Mockito.when(gymSessionRepo.save(Mockito.any(GymSession.class))).thenReturn(s);

        mockMvc.perform(post("/api/gymsessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseName").value("Deadlift"));
    }

    @Test
    void createGymSession_missingAthlete_shouldFail() throws Exception {
        GymSession s = new GymSession();
        s.setExerciseName("Pull Ups");

        mockMvc.perform(post("/api/gymsessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGymSession_athleteNotFound_shouldFail() throws Exception {
        Athlete a = new Athlete();
        a.setId(99);

        GymSession s = new GymSession();
        s.setAthlete(a);

        Mockito.when(athleteRepo.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/gymsessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s)))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       DELETE /api/gymsessions/{id}
    ===================================================== */
    @Test
    void deleteGymSession_shouldReturnNoContent() throws Exception {
        Mockito.when(gymSessionRepo.existsById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/gymsessions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteGymSession_notFound() throws Exception {
        Mockito.when(gymSessionRepo.existsById(5)).thenReturn(false);

        mockMvc.perform(delete("/api/gymsessions/5"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       GET /api/gymsessions/filter
    ===================================================== */
    @Test
    void filterGymSessions_shouldReturnFilteredList() throws Exception {
        Athlete a = new Athlete();
        a.setId(2);

        GymSession s = new GymSession();
        s.setExerciseName("Bench Press");
        s.setCategory("Strength");
        s.setSessionDate(LocalDate.of(2024, 5, 1));
        s.setAthlete(a);

        Mockito.when(gymSessionRepo.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/gymsessions/filter")
                        .param("athleteId", "2")
                        .param("category", "Strength")
                        .param("search", "bench")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].exerciseName").value("Bench Press"));
    }
}
