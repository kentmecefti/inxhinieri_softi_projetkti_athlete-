package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/athletes")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ATHLETE','ROLE_COACH')")
public class AthleteController {

    private final AthleteRepository repo;
    private final ResultRepository resultRepo;
    private final JumpResultRepository jumpResultRepo;
    private final ThrowResultRepository throwResultRepo;
    private final GymSessionRepository gymSessionRepo;
    private final SessionRepository sessionRepo;
    private final CoachAthleteRelationRepository relationRepo;
    private final UserRepository userRepo;
    private final CoachRepository coachRepo;


    public AthleteController(AthleteRepository repo,
                             ResultRepository resultRepo,
                             JumpResultRepository jumpResultRepo,
                             ThrowResultRepository throwResultRepo,
                             GymSessionRepository gymSessionRepo,
                             SessionRepository sessionRepo,
                             CoachAthleteRelationRepository relationRepo,
                             UserRepository userRepo,
                             CoachRepository coachRepo) {
        this.repo = repo;
        this.resultRepo = resultRepo;
        this.jumpResultRepo = jumpResultRepo;
        this.throwResultRepo = throwResultRepo;
        this.gymSessionRepo = gymSessionRepo;
        this.sessionRepo = sessionRepo;
        this.relationRepo = relationRepo;
        this.userRepo = userRepo;
        this.coachRepo = coachRepo;
    }

    // ===== GET all athletes =====
    @GetMapping
    public List<Athlete> all() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Athlete> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Athlete athlete = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // ADMIN → always allowed
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(athlete);
        }

        // ATHLETE → only self
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ATHLETE"))) {

            Athlete logged = repo.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

            if (!logged.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(athlete);
        }

        // COACH → must be linked
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"))) {

            Coach coach = coachRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

            boolean linked = relationRepo
                    .existsByCoachIdAndAthleteIdAndStatusId(coach.getId(), athlete.getId(), 1);

            if (!linked) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return ResponseEntity.ok(athlete);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }



    // ===== CREATE new athlete =====
    @PostMapping
    public Athlete create(@RequestBody Map<String, Object> body) {
        Athlete athlete = new Athlete();

        athlete.setName((String) body.get("name"));
        athlete.setLastname((String) body.get("lastname"));
        athlete.setGender((String) body.get("gender"));
        athlete.setBirthDate(body.get("birthDate") != null ? Date.valueOf((String) body.get("birthDate")) : null);
        athlete.setAge((body.get("age") instanceof Number) ? ((Number) body.get("age")).intValue() : null);
        athlete.setAthWeight((body.get("athWeight") instanceof Number) ? ((Number) body.get("athWeight")).doubleValue() : null);
        athlete.setAthHeight((body.get("athHeight") instanceof Number) ? ((Number) body.get("athHeight")).doubleValue() : null);
        athlete.setCategory((String) body.get("category"));
        athlete.setPerformance((String) body.getOrDefault("performance", "average"));
        athlete.setClub((String) body.get("club"));
        athlete.setCountry((String) body.get("country"));
        athlete.setCity((String) body.get("city"));
        athlete.setUpdatedAt(LocalDateTime.now());

        if (body.get("user_id") instanceof Number)
            athlete.setUserId(((Number) body.get("user_id")).intValue());

        return repo.save(athlete);
    }

    // ===== UPDATE athlete =====
    @PutMapping("/{id}")
    public Athlete update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Athlete athlete = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        if (body.containsKey("name")) athlete.setName((String) body.get("name"));
        if (body.containsKey("lastname")) athlete.setLastname((String) body.get("lastname"));
        if (body.containsKey("gender")) athlete.setGender((String) body.get("gender"));
        if (body.containsKey("birthDate") && body.get("birthDate") != null)
            athlete.setBirthDate(Date.valueOf((String) body.get("birthDate")));
        if (body.containsKey("age") && body.get("age") instanceof Number)
            athlete.setAge(((Number) body.get("age")).intValue());
        if (body.containsKey("athWeight") && body.get("athWeight") instanceof Number)
            athlete.setAthWeight(((Number) body.get("athWeight")).doubleValue());
        if (body.containsKey("athHeight") && body.get("athHeight") instanceof Number)
            athlete.setAthHeight(((Number) body.get("athHeight")).doubleValue());
        if (body.containsKey("category")) athlete.setCategory((String) body.get("category"));
        if (body.containsKey("performance")) athlete.setPerformance((String) body.get("performance"));
        if (body.containsKey("club")) athlete.setClub((String) body.get("club"));
        if (body.containsKey("country")) athlete.setCountry((String) body.get("country"));
        if (body.containsKey("city")) athlete.setCity((String) body.get("city"));

        athlete.setUpdatedAt(LocalDateTime.now());
        return repo.save(athlete);
    }

    // ===== DELETE athlete =====
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        if (!repo.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found");
        repo.deleteById(id);
        return "Athlete deleted successfully";
    }

    // ===== GET all coaches linked to this athlete =====
    @GetMapping("/{id}/coaches")
    public List<Coach> getCoachesByAthlete(@PathVariable Integer id) {
        Athlete athlete = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));
        return new ArrayList<>(athlete.getCoaches());
    }

    // ===== GET athlete by user_id =====
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Athlete> getByUserId(@PathVariable Integer userId) {
        Athlete athlete = repo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found for this user"));
        return ResponseEntity.ok(athlete);
    }

    // ===== GET coaches by athlete's decision (accept/reject/pending) =====
    @GetMapping("/{athleteId}/coaches/decision/{status}")
    public ResponseEntity<List<Map<String, Object>>> getCoachesByAthleteDecision(
            @PathVariable Integer athleteId,
            @PathVariable String status) {

        Athlete athlete = repo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        List<Map<String, Object>> result = new ArrayList<>();

        for (CoachAthleteRelation rel : athlete.getCoachAthleteRelations()) {
            String decision = rel.getStatus() != null ? rel.getStatus().getStatusName() : "pending";
            if (decision.equalsIgnoreCase(status)) {
                Coach coach = rel.getCoach();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("coach_id", coach.getId());
                map.put("name", coach.getName());
                map.put("lastname", coach.getLastname());
                map.put("experienceYears", coach.getExperienceYears());
                map.put("specialization", coach.getSpecialization());
                map.put("status", decision);
                map.put("userId", coach.getUserId());
                result.add(map);
            }
        }

        return ResponseEntity.ok(result);
    }
}
