package com.example.athleteresults.services;

import com.example.athleteresults.entities.Plan;
import com.example.athleteresults.repositories.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // ====== GET ALL ======
    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    // ====== GET ONE ======
    public Optional<Plan> getPlanById(Integer id) {
        return planRepository.findById(id);
    }

    public List<Plan> getPlansByAthlete(Integer athleteId) {
        return planRepository.findByAthleteIdOrderByPlanDateAsc(athleteId);
    }

    public List<Plan> getPlansByCoach(Integer coachId) {
        return planRepository.findByCoachIdOrderByPlanDateAsc(coachId);
    }


    // ====== SAVE (CREATE or UPDATE) ======
    public Plan savePlan(Plan plan) {
        // 🧩 Only check for duplicates if it's a NEW plan (no ID yet)
        if (plan.getId() == null) {
            this.checkPlanAvailability(plan.getAthlete().getId(), plan.getPlanDate());
        }
        return planRepository.save(plan);
    }

    // ====== DELETE ======
    public void deletePlan(Integer id) {
        planRepository.deleteById(id);
    }

    // ====== DUPLICATE CHECK ======
    public void checkPlanAvailability(Integer athleteId, LocalDate planDate) {
        List<Plan> plans = planRepository.findByDateAndAthleteId(planDate, athleteId);
        if (!plans.isEmpty()) {
            throw new RuntimeException(
                    "Athlete with id: " + athleteId +
                            " already has a plan assigned for date: " + planDate
            );
        }
    }
}
