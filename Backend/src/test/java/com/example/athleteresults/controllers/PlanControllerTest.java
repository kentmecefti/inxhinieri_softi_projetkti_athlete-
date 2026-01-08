package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import com.example.athleteresults.security.JwtAuthFilter;
import com.example.athleteresults.services.PlanService;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PlanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCKS ===== */
    @MockBean private PlanService planService;
    @MockBean private PlanRepository planRepo;
    @MockBean private AthleteRepository athleteRepo;
    @MockBean private CoachRepository coachRepo;

    /* =====================================================
       GET /api/plans
    ===================================================== */
    @Test
    void getAllPlans_shouldReturnList() throws Exception {
        Plan p = samplePlan();

        Mockito.when(planService.getAllPlans()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].athleteName").value("Athlete A"));
    }

    /* =====================================================
       GET /api/plans/{id}
    ===================================================== */
    @Test
    void getPlanById_shouldReturnPlan() throws Exception {
        Plan p = samplePlan();

        Mockito.when(planService.getPlanById(1)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coachName").value("Coach C"));
    }

    /* =====================================================
       GET /api/plans/athlete/{id}
    ===================================================== */
    @Test
    void getPlansByAthlete_shouldReturnList() throws Exception {
        Plan p = samplePlan();

        Mockito.when(planService.getPlansByAthlete(10)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/plans/athlete/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].athleteId").value(10));
    }

    /* =====================================================
       POST /api/plans
    ===================================================== */
    @Test
    void createPlan_shouldReturnCreated() throws Exception {
        Athlete a = athlete();
        Coach c = coach();
        Plan p = samplePlan();

        Mockito.when(athleteRepo.findById(10)).thenReturn(Optional.of(a));
        Mockito.when(coachRepo.findById(5)).thenReturn(Optional.of(c));
        Mockito.when(planService.savePlan(Mockito.any(Plan.class))).thenReturn(p);

        PlanController.PlanRequest req =
                new PlanController.PlanRequest(
                        10, 5, LocalDate.now(),
                        "Pred", "Actual", "Notes"
                );

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictionPlan").value("Pred"));
    }

    /* =====================================================
       PUT /api/plans/{id}
    ===================================================== */
    @Test
    void updatePlan_shouldReturnUpdated() throws Exception {
        Plan existing = samplePlan();

        Mockito.when(planService.getPlanById(1)).thenReturn(Optional.of(existing));
        Mockito.when(athleteRepo.findById(10)).thenReturn(Optional.of(athlete()));
        Mockito.when(coachRepo.findById(5)).thenReturn(Optional.of(coach()));
        Mockito.when(planService.savePlan(Mockito.any(Plan.class))).thenReturn(existing);

        PlanController.PlanRequest req =
                new PlanController.PlanRequest(
                        10, 5, LocalDate.now(),
                        "Updated", "Actual", "Notes"
                );

        mockMvc.perform(put("/api/plans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictionPlan").value("Updated"));
    }

    /* =====================================================
       PATCH /api/plans/{id}/actual
    ===================================================== */
    @Test
    void patchActual_shouldUpdateOnlyActual() throws Exception {
        Plan p = samplePlan();

        Mockito.when(planService.getPlanById(1)).thenReturn(Optional.of(p));
        Mockito.when(planService.savePlan(Mockito.any(Plan.class))).thenReturn(p);

        mockMvc.perform(patch("/api/plans/1/actual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualPlan\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualPlan").value("DONE"));
    }

    /* =====================================================
       DELETE /api/plans/{id}
    ===================================================== */
    @Test
    void deletePlan_shouldReturnOk() throws Exception {
        Mockito.doNothing().when(planService).deletePlan(1);

        mockMvc.perform(delete("/api/plans/1"))
                .andExpect(status().isOk());
    }

    /* =====================================================
       POST /api/plans/coach/{id}/send
    ===================================================== */
    @Test
    void sendPlanToMultipleAthletes_shouldReturnList() throws Exception {
        Coach c = coach();
        Athlete a = athlete();
        Plan p = samplePlan();

        Mockito.when(coachRepo.findById(5)).thenReturn(Optional.of(c));
        Mockito.when(athleteRepo.findAllById(List.of(10)))
                .thenReturn(List.of(a));
        Mockito.when(planService.savePlan(Mockito.any(Plan.class)))
                .thenReturn(p);

        PlanController.SendPlanRequest req =
                new PlanController.SendPlanRequest(
                        List.of(10),
                        LocalDate.now(),
                        "Pred",
                        "Actual",
                        "Notes"
                );

        mockMvc.perform(post("/api/plans/coach/5/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* =====================================================
       GET /api/plans/filter   (IMPORTANT FIX)
    ===================================================== */
    @Test
    void filterPlans_shouldReturnList() throws Exception {
        Plan p = samplePlan();

        Mockito.when(
                planRepo.findAll(
                        Mockito.<org.springframework.data.jpa.domain.Specification<Plan>>any(),
                        Mockito.any(org.springframework.data.domain.Sort.class)
                )
        ).thenReturn(List.of(p));

        mockMvc.perform(get("/api/plans/filter")
                        .param("athleteId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].athleteId").value(10));
    }

    /* =====================================================
       ===== HELPER METHODS =====
    ===================================================== */

    private Athlete athlete() {
        Athlete a = new Athlete();
        a.setId(10);
        a.setName("Athlete A");
        return a;
    }

    private Coach coach() {
        Coach c = new Coach();
        c.setId(5);
        c.setName("Coach C");
        return c;
    }

    private Plan samplePlan() {
        Plan p = new Plan();
        p.setId(1);
        p.setAthlete(athlete());
        p.setCoach(coach());
        p.setPlanDate(LocalDate.now());
        p.setPredictionPlan("Pred");
        p.setActualPlan("Actual");
        p.setNotes("Notes");
        return p;
    }
}
