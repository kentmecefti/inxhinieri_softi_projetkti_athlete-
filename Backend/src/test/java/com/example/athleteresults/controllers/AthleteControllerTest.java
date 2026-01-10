package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import com.example.athleteresults.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AthleteController.class)
@AutoConfigureMockMvc(addFilters = false)   // 🔥 REQUIRED
class AthleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AthleteRepository repo;
    @MockBean private ResultRepository resultRepo;
    @MockBean private JumpResultRepository jumpResultRepo;
    @MockBean private ThrowResultRepository throwResultRepo;
    @MockBean private GymSessionRepository gymSessionRepo;
    @MockBean private SessionRepository sessionRepo;
    @MockBean private CoachAthleteRelationRepository relationRepo;
    @MockBean private UserRepository userRepo;
    @MockBean private CoachRepository coachRepo;

    // Disable JWT filter completely
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    /* =====================================================
       GET /api/athletes (ADMIN)
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_shouldReturn200() throws Exception {
        when(repo.findAll()).thenReturn(List.of(new Athlete()));

        mockMvc.perform(get("/api/athletes"))
                .andExpect(status().isOk());
    }

    /* =====================================================
       GET /api/athletes/{id} (ADMIN)
    ===================================================== */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getById_admin_shouldReturn200() throws Exception {

        com.example.athleteresults.entities.User user =
                new com.example.athleteresults.entities.User();
        user.setId(1);

        Athlete athlete = new Athlete();
        athlete.setId(1);

        when(userRepo.findByUsername("admin"))
                .thenReturn(Optional.of(user));
        when(repo.findById(1))
                .thenReturn(Optional.of(athlete));

        mockMvc.perform(get("/api/athletes/1"))
                .andExpect(status().isOk());
    }

    /* =====================================================
       ATHLETE accessing other athlete → 403
    ===================================================== */
    @Test
    @WithMockUser(username = "athlete", roles = "ATHLETE")
    void athlete_accessing_other_shouldReturn403() throws Exception {

        com.example.athleteresults.entities.User user =
                new com.example.athleteresults.entities.User();
        user.setId(5);

        Athlete logged = new Athlete();
        logged.setId(1);
        logged.setUserId(5);

        Athlete other = new Athlete();
        other.setId(99);
        other.setUserId(999);

        when(userRepo.findByUsername("athlete"))
                .thenReturn(Optional.of(user));
        when(repo.findByUserId(5))
                .thenReturn(Optional.of(logged));
        when(repo.findById(99))
                .thenReturn(Optional.of(other));

        mockMvc.perform(get("/api/athletes/99"))
                .andExpect(status().isForbidden());
    }

    /* =====================================================
       COACH not linked to athlete → 403
    ===================================================== */
    @Test
    @WithMockUser(username = "coach", roles = "COACH")
    void coach_notLinked_shouldReturn403() throws Exception {

        com.example.athleteresults.entities.User user =
                new com.example.athleteresults.entities.User();
        user.setId(10);

        Coach coach = new Coach();
        coach.setId(3);

        Athlete athlete = new Athlete();
        athlete.setId(1);

        when(userRepo.findByUsername("coach"))
                .thenReturn(Optional.of(user));
        when(coachRepo.findByUserId(10))
                .thenReturn(Optional.of(coach));
        when(repo.findById(1))
                .thenReturn(Optional.of(athlete));
        when(relationRepo.existsByCoachIdAndAthleteIdAndStatusId(3, 1, 1))
                .thenReturn(false);

        mockMvc.perform(get("/api/athletes/1"))
                .andExpect(status().isForbidden());
    }

    /* =====================================================
       POST /api/athletes
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void create_shouldReturn200() throws Exception {

        when(repo.save(any(Athlete.class)))
                .thenReturn(new Athlete());

        mockMvc.perform(post("/api/athletes")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"Kent\"}"))
                .andExpect(status().isOk());
    }

    /* =====================================================
       PUT /api/athletes/{id}
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn200() throws Exception {

        when(repo.findById(1))
                .thenReturn(Optional.of(new Athlete()));
        when(repo.save(any(Athlete.class)))
                .thenReturn(new Athlete());

        mockMvc.perform(put("/api/athletes/1")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"New\"}"))
                .andExpect(status().isOk());
    }

    /* =====================================================
       DELETE /api/athletes/{id}
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn200() throws Exception {

        when(repo.existsById(1))
                .thenReturn(true);

        mockMvc.perform(delete("/api/athletes/1")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
