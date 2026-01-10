package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Plan;
import com.example.athleteresults.entities.Athlete;
import com.example.athleteresults.entities.Coach;
import com.example.athleteresults.entities.Result;
import com.example.athleteresults.repositories.PlanRepository;
import com.example.athleteresults.services.PlanService;
import com.example.athleteresults.repositories.AthleteRepository;
import com.example.athleteresults.repositories.CoachRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
public class PlanController {

    private final PlanService planService;
    private final AthleteRepository athleteRepository;
    private final CoachRepository coachRepository;
    private final PlanRepository repo;

    public PlanController(PlanService planService, AthleteRepository athleteRepository, CoachRepository coachRepository, PlanRepository repo) {
        this.planService = planService;
        this.athleteRepository = athleteRepository;
        this.coachRepository = coachRepository;
        this.repo = repo;
    }

    // ====== GET all plans ======
    @GetMapping
    public List<PlanDTO> getAllPlans() {
        return planService.getAllPlans().stream()
                .map(PlanDTO::fromEntity)
                .toList();
    }

    // ====== GET by ID ======
    @GetMapping("/{id}")
    public PlanDTO getPlanById(@PathVariable Integer id) {
        Plan plan = planService.getPlanById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id " + id));
        return PlanDTO.fromEntity(plan);
    }

    // ====== GET by Athlete ======
    @GetMapping("/athlete/{athleteId}")
    public List<PlanDTO> getPlansByAthlete(@PathVariable Integer athleteId) {
        return planService.getPlansByAthlete(athleteId).stream()
                .map(PlanDTO::fromEntity)
                .toList();
    }

    // ====== GET by Coach ======
    @GetMapping("/coach/{coachId}")
    public List<PlanDTO> getPlansByCoach(@PathVariable Integer coachId) {
        return planService.getPlansByCoach(coachId).stream()
                .map(PlanDTO::fromEntity)
                .toList();
    }

    // ====== CREATE ======
    @PostMapping
    public PlanDTO createPlan(@RequestBody PlanRequest req) {
        Athlete athlete = athleteRepository.findById(req.athleteId())
                .orElseThrow(() -> new RuntimeException("Athlete not found"));
        Coach coach = null;
        if (req.coachId() != null) {
            coach = coachRepository.findById(req.coachId())
                    .orElseThrow(() -> new RuntimeException("Coach not found"));
        }


        Plan plan = new Plan();
        plan.setAthlete(athlete);
        plan.setCoach(coach);
        plan.setPlanDate(req.planDate());
        plan.setPredictionPlan(req.predictionPlan());
        plan.setActualPlan(req.actualPlan());
        plan.setNotes(req.notes());

        return PlanDTO.fromEntity(planService.savePlan(plan));
    }

    // ====== UPDATE ======
    @PutMapping("/{id}")
    public PlanDTO updatePlan(@PathVariable Integer id, @RequestBody PlanRequest req) {
        Plan existing = planService.getPlanById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id " + id));

        Athlete athlete = athleteRepository.findById(req.athleteId())
                .orElseThrow(() -> new RuntimeException("Athlete not found"));
        Coach coach = null;
        if (req.coachId() != null) {
            coach = coachRepository.findById(req.coachId())
                    .orElseThrow(() -> new RuntimeException("Coach not found"));
        }

        existing.setAthlete(athlete);
        existing.setCoach(coach);
        existing.setPlanDate(req.planDate());
        existing.setPredictionPlan(req.predictionPlan());
        existing.setActualPlan(req.actualPlan());
        existing.setNotes(req.notes());

        return PlanDTO.fromEntity(planService.savePlan(existing));
    }

    // ====== DELETE ======
    @DeleteMapping("/{id}")
    public void deletePlan(@PathVariable Integer id) {
        planService.deletePlan(id);
    }

    // ====== SEND TO MULTIPLE ======
    @PostMapping("/coach/{coachId}/send")
    public List<PlanDTO> sendPlanToAthletes(
            @PathVariable Integer coachId,
            @RequestBody SendPlanRequest req) {

        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("Coach not found"));

        List<Athlete> athletes = athleteRepository.findAllById(req.athleteIds());
        if (athletes.isEmpty()) {
            throw new RuntimeException("No valid athletes found");
        }

        return athletes.stream().map(athlete -> {
            Plan p = new Plan();
            p.setCoach(coach);
            p.setAthlete(athlete);
            p.setPlanDate(req.planDate());
            p.setPredictionPlan(req.predictionPlan());
            p.setActualPlan(req.actualPlan());
            p.setNotes(req.notes());
            return PlanDTO.fromEntity(planService.savePlan(p));
        }).toList();
    }

    @GetMapping("/filter")
    public List<PlanDTO> filterResults(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) Integer coachId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Sort sort = Sort.by("planDate").ascending();
        return repo.findAll(buildSpec(athleteId, coachId, from, to), sort)
                .stream()
                .map(PlanDTO::fromEntity)
                .toList();
    }

    // add inside PlanController
    @PatchMapping("/{id}/actual")
    public PlanDTO patchActual(@PathVariable Integer id, @RequestBody ActualOnly body) {
        Plan p = planService.getPlanById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id " + id));
        p.setActualPlan(body.actualPlan());
        return PlanDTO.fromEntity(planService.savePlan(p));
    }

    // record for request body
    public record ActualOnly(String actualPlan) {}


    // ===== Helper — dynamic Specification builder =====
    private Specification<Plan> buildSpec(
            Integer athleteId, Integer coachId, LocalDate from, LocalDate to
    ) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (athleteId != null)
                p = cb.and(p, cb.equal(root.get("athlete").get("id"), athleteId));

            if (coachId != null)
                p = cb.and(p, cb.equal(root.get("coach").get("id"), coachId));

            if (from != null)
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("planDate"), from));

            if (to != null)
                p = cb.and(p, cb.lessThan(root.get("planDate"), to.plusDays(1)));

            return p;
        };
    }


    // ====== RECORD TYPES ======
    public record PlanRequest(
            Integer athleteId,
            Integer coachId,
            java.time.LocalDate planDate,
            String predictionPlan,
            String actualPlan,
            String notes
    ) {}

    public record SendPlanRequest(
            List<Integer> athleteIds,
            java.time.LocalDate planDate,
            String predictionPlan,
            String actualPlan,
            String notes
    ) {}

    // ====== DTO (Data Transfer Object) ======
    public record PlanDTO(
            Integer id,
            java.time.LocalDate planDate,
            String predictionPlan,
            String actualPlan,
            String notes,
            String athleteName,
            String coachName,
            Integer athleteId,
            Integer coachId
    ) {
        public static PlanDTO fromEntity(Plan p) {
            return new PlanDTO(
                    p.getId(),
                    p.getPlanDate(),
                    p.getPredictionPlan(),
                    p.getActualPlan(),
                    p.getNotes(),
                    p.getAthlete() != null ? p.getAthlete().getName() : null,
                    p.getCoach() != null ? p.getCoach().getName() : null,
                    p.getAthlete() != null ? p.getAthlete().getId() : null,
                    p.getCoach() != null ? p.getCoach().getId() : null
            );
        }
    }

    // ====== EXCEPTION HANDLER ======
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String handleRuntime(RuntimeException ex) {
        return ex.getMessage();
    }
}
