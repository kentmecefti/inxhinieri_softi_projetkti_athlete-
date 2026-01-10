package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Session;
import com.example.athleteresults.repositories.SessionRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SessionController
 * Security & JWT are disabled
 */
@WebMvcTest(
        controllers = SessionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORY ===== */
    @MockBean
    private SessionRepository repo;

    /* =====================================================
       GET /api/sessions
    ===================================================== */
    @Test
    void getAllSessions_shouldReturnList() throws Exception {
        Session s = new Session();

        Mockito.when(repo.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       GET /api/sessions/athlete/{athleteId}
    ===================================================== */
    @Test
    void getSessionsByAthlete_withoutDate_shouldReturnList() throws Exception {
        Session s = new Session();

        Mockito.when(repo.findByAthleteIdOrderByRunDateDesc(10))
                .thenReturn(List.of(s));

        mockMvc.perform(get("/api/sessions/athlete/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getSessionsByAthlete_withDate_shouldReturnList() throws Exception {
        Session s = new Session();
        LocalDate date = LocalDate.of(2024, 5, 1);

        Mockito.when(repo.findByAthleteIdAndRunDate(10, date))
                .thenReturn(List.of(s));

        mockMvc.perform(get("/api/sessions/athlete/10")
                        .param("date", "2024-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       GET /api/sessions/{id}
    ===================================================== */
    @Test
    void getSessionById_shouldReturnSession() throws Exception {
        Session s = new Session();

        Mockito.when(repo.findById(5)).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/sessions/5"))
                .andExpect(status().isOk());
    }

    @Test
    void getSessionById_notFound() throws Exception {
        Mockito.when(repo.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sessions/99"))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       POST /api/sessions
    ===================================================== */
    @Test
    void createSession_shouldReturnCreated() throws Exception {
        Session s = new Session();
        s.setAthleteId(1);
        s.setRunDate(LocalDate.now());

        Mockito.when(repo.save(Mockito.any(Session.class))).thenReturn(s);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s)))
                .andExpect(status().isCreated());
    }

    @Test
    void createSession_missingRequiredFields_shouldFail() throws Exception {
        Session s = new Session();

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s)))
                .andExpect(status().isBadRequest());
    }

    /* =====================================================
       PUT /api/sessions/{id}
    ===================================================== */
    @Test
    void updateSession_shouldReturnUpdated() throws Exception {
        Session existing = new Session();

        Session updated = new Session();
        updated.setRunDate(LocalDate.now());

        Mockito.when(repo.findById(1)).thenReturn(Optional.of(existing));
        Mockito.when(repo.save(Mockito.any(Session.class))).thenReturn(existing);

        mockMvc.perform(put("/api/sessions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk());
    }

    @Test
    void updateSession_notFound() throws Exception {
        Session updated = new Session();

        Mockito.when(repo.findById(77)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/sessions/77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound());
    }

    /* =====================================================
       DELETE /api/sessions/{id}
    ===================================================== */
    @Test
    void deleteSession_shouldReturnOk() throws Exception {
        Mockito.when(repo.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/sessions/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Session deleted successfully"));
    }

    @Test
    void deleteSession_notFound() throws Exception {
        Mockito.when(repo.existsById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/sessions/99"))
                .andExpect(status().isNotFound());
    }
}
