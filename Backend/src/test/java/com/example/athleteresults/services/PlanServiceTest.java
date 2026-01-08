package com.example.athleteresults.services;

import com.example.athleteresults.entities.Plan;
import com.example.athleteresults.repositories.PlanRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanServiceTest {

    private final PlanRepository repo = mock(PlanRepository.class);
    private final PlanService service = new PlanService(repo);

    /* =====================================================
       GET ALL
    ===================================================== */
    @Test
    void getAllPlans_shouldReturnList() {
        when(repo.findAll()).thenReturn(List.of(new Plan()));

        assertEquals(1, service.getAllPlans().size());
    }

    /* =====================================================
       GET ONE
    ===================================================== */
    @Test
    void getPlanById_shouldReturnOptional() {
        Plan p = new Plan();
        p.setId(1);

        when(repo.findById(1)).thenReturn(Optional.of(p));

        Optional<Plan> result = service.getPlanById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    /* =====================================================
       GET BY ATHLETE
    ===================================================== */
    @Test
    void getPlansByAthlete_shouldUseRepository() {
        when(repo.findByAthleteIdOrderByPlanDateAsc(10))
                .thenReturn(List.of(new Plan()));

        assertEquals(1, service.getPlansByAthlete(10).size());
    }

    /* =====================================================
       SAVE — NEW PLAN (NO DUPLICATE)
    ===================================================== */
    @Test
    void savePlan_newPlan_noDuplicate_shouldSave() {
        Plan p = new Plan();
        p.setAthlete(TestData.athlete(10));
        p.setPlanDate(LocalDate.now());

        when(repo.findByDateAndAthleteId(any(), any()))
                .thenReturn(List.of());

        when(repo.save(p)).thenReturn(p);

        Plan saved = service.savePlan(p);

        assertNotNull(saved);
        verify(repo).save(p);
    }

    /* =====================================================
       SAVE — NEW PLAN (DUPLICATE EXISTS)
    ===================================================== */
    @Test
    void savePlan_duplicate_shouldThrow() {
        Plan p = new Plan();
        p.setAthlete(TestData.athlete(10));
        p.setPlanDate(LocalDate.now());

        when(repo.findByDateAndAthleteId(any(), any()))
                .thenReturn(List.of(new Plan()));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.savePlan(p)
        );

        assertTrue(ex.getMessage().contains("already has a plan"));
        verify(repo, never()).save(any());
    }

    /* =====================================================
       SAVE — EXISTING PLAN (ID PRESENT)
    ===================================================== */
    @Test
    void savePlan_existingPlan_shouldSkipDuplicateCheck() {
        Plan p = new Plan();
        p.setId(1);
        p.setAthlete(TestData.athlete(10));
        p.setPlanDate(LocalDate.now());

        when(repo.save(p)).thenReturn(p);

        Plan saved = service.savePlan(p);

        assertNotNull(saved);
        verify(repo, never()).findByDateAndAthleteId(any(), any());
        verify(repo).save(p);
    }

    /* =====================================================
       DELETE
    ===================================================== */
    @Test
    void deletePlan_shouldCallRepo() {
        service.deletePlan(1);
        verify(repo).deleteById(1);
    }

    /* =====================================================
       TEST DATA
    ===================================================== */
    static class TestData {
        static com.example.athleteresults.entities.Athlete athlete(Integer id) {
            com.example.athleteresults.entities.Athlete a =
                    new com.example.athleteresults.entities.Athlete();
            a.setId(id);
            return a;
        }
    }
}
