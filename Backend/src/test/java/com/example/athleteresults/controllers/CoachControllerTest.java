package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CoachController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class CoachControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORIES ===== */
    @MockBean private CoachRepository coachRepo;
    @MockBean private AthleteRepository athleteRepo;
    @MockBean private UserRepository userRepo;
    @MockBean private CoachAthleteRelationRepository relationRepo;
    @MockBean private StatusRepository statusRepo;

    /* =====================================================
       GET /api/coaches  (ADMIN)
    ===================================================== */
    @Test
    void getAllCoaches_admin_shouldReturnList() throws Exception {
        Coach coach = sampleCoach();

        Mockito.when(coachRepo.findAll()).thenReturn(List.of(coach));

        mockMvc.perform(get("/api/coaches")
                        .principal(adminPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("John"));
    }

    /* =====================================================
       GET /api/coaches/{id}  (COACH sees himself)
    ===================================================== */
    @Test
    void getCoach_self_shouldReturnCoach() throws Exception {
        Coach coach = sampleCoach();
        com.example.athleteresults.entities.User dbUser = dbUser();

        Mockito.when(userRepo.findByUsername("coach1"))
                .thenReturn(Optional.of(dbUser));
        Mockito.when(coachRepo.findByUserId(10))
                .thenReturn(Optional.of(coach));
        Mockito.when(coachRepo.findById(1))
                .thenReturn(Optional.of(coach));

        mockMvc.perform(get("/api/coaches/1")
                        .principal(coachPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John"));
    }

    /* =====================================================
       GET /api/coaches/{id}  (COACH sees other → FORBIDDEN)
    ===================================================== */
    @Test
    void getCoach_otherCoach_shouldBeForbidden() throws Exception {
        Coach loggedCoach = sampleCoach();
        Coach otherCoach = sampleCoach();
        otherCoach.setId(2);

        Mockito.when(userRepo.findByUsername("coach1"))
                .thenReturn(Optional.of(dbUser()));
        Mockito.when(coachRepo.findByUserId(10))
                .thenReturn(Optional.of(loggedCoach));
        Mockito.when(coachRepo.findById(2))
                .thenReturn(Optional.of(otherCoach));

        mockMvc.perform(get("/api/coaches/2")
                        .principal(coachPrincipal()))
                .andExpect(status().isForbidden());
    }

    /* =====================================================
       POST /api/coaches
    ===================================================== */
    @Test
    void createCoach_shouldReturnCreated() throws Exception {
        Coach coach = sampleCoach();
        coach.setId(99);

        Mockito.when(userRepo.findById(10))
                .thenReturn(Optional.of(dbUser()));
        Mockito.when(coachRepo.save(Mockito.any(Coach.class)))
                .thenReturn(coach);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", 10);
        body.put("name", "John");

        mockMvc.perform(post("/api/coaches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .principal(adminPrincipal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99));
    }

    /* =====================================================
       DELETE /api/coaches/{id}
    ===================================================== */
    @Test
    void deleteCoach_shouldReturnOk() throws Exception {
        Mockito.when(coachRepo.existsById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/coaches/1")
                        .principal(adminPrincipal()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Coach deleted")));
    }

    /* =====================================================
       GET /api/coaches/me
    ===================================================== */
    @Test
    void getLoggedCoach_shouldReturnCoach() throws Exception {
        Mockito.when(userRepo.findByUsername("coach1"))
                .thenReturn(Optional.of(dbUser()));
        Mockito.when(coachRepo.findByUserId(10))
                .thenReturn(Optional.of(sampleCoach()));

        mockMvc.perform(get("/api/coaches/me")
                        .principal(coachPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John"));
    }

    /* =====================================================
       ===== HELPER METHODS =====
    ===================================================== */

    private Coach sampleCoach() {
        Coach c = new Coach("John", 10);
        c.setId(1);
        c.setLastname("Doe");
        c.setUpdatedAt(LocalDateTime.now());
        c.setCoachAthleteRelations(Set.of());
        return c;
    }

    private com.example.athleteresults.entities.User dbUser() {
        com.example.athleteresults.entities.User u =
                new com.example.athleteresults.entities.User();
        u.setId(10);
        u.setUsername("coach1");
        return u;
    }

    private java.security.Principal coachPrincipal() {
        return () -> "coach1";
    }

    private java.security.Principal adminPrincipal() {
        return () -> "admin";
    }
}
