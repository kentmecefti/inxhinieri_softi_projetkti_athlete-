package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/coaches")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_COACH')")
public class CoachController {

    private final CoachRepository coachRepo;
    private final AthleteRepository athleteRepo;
    private final UserRepository userRepo;
    private final CoachAthleteRelationRepository relationRepo;
    private final StatusRepository statusRepo;

    public CoachController(CoachRepository coachRepo,
                           AthleteRepository athleteRepo,
                           UserRepository userRepo,
                           CoachAthleteRelationRepository relationRepo,
                           StatusRepository statusRepo) {
        this.coachRepo = coachRepo;
        this.athleteRepo = athleteRepo;
        this.userRepo = userRepo;
        this.relationRepo = relationRepo;
        this.statusRepo = statusRepo;
    }

    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (Coach coach : coachRepo.findAll()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", coach.getId());
            response.put("name", coach.getName());
            response.put("lastname", coach.getLastname());
            response.put("gender", coach.getGender());
            response.put("experienceYears", coach.getExperienceYears());
            response.put("specialization", coach.getSpecialization());
            response.put("phone", coach.getPhone());
            response.put("club", coach.getClub());
            response.put("country", coach.getCountry());
            response.put("userId", coach.getUserId());
            response.put("updatedAt", coach.getUpdatedAt());

            List<Map<String, Object>> athletesWithStatus = new ArrayList<>();
            for (CoachAthleteRelation rel : coach.getCoachAthleteRelations()) {
                Athlete a = rel.getAthlete();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("athlete_id", a.getId());
                map.put("name", a.getName());
                map.put("lastname", a.getLastname());
                map.put("athWeight", a.getAthWeight());
                map.put("athHeight", a.getAthHeight());
                map.put("category", a.getCategory());
                map.put("performance", a.getPerformance());
                String relStatus = rel.getStatus() != null ? rel.getStatus().getStatusName() : "pending";
                map.put("relation_status", relStatus);
                map.put("userId", a.getUserId());
                athletesWithStatus.add(map);
            }

            response.put("athletesWithStatus", athletesWithStatus);
            responseList.add(response);
        }

        return ResponseEntity.ok(responseList);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        
        if (userDetails == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");

        
        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isCoach = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));

        
        if (isCoach && !isAdmin) {

            Coach loggedCoach = coachRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a coach"));

            
            if (!loggedCoach.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot access another coach's profile.");
            }
        }

        

        
        Coach coach = coachRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", coach.getId());
        response.put("name", coach.getName());
        response.put("lastname", coach.getLastname());
        response.put("gender", coach.getGender());
        response.put("experienceYears", coach.getExperienceYears());
        response.put("specialization", coach.getSpecialization());
        response.put("phone", coach.getPhone());
        response.put("club", coach.getClub());
        response.put("country", coach.getCountry());
        response.put("userId", coach.getUserId());
        response.put("updatedAt", coach.getUpdatedAt());

        List<Map<String, Object>> athletesWithStatus = new ArrayList<>();
        for (CoachAthleteRelation rel : coach.getCoachAthleteRelations()) {
            Athlete a = rel.getAthlete();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("athlete_id", a.getId());
            map.put("name", a.getName());
            map.put("lastname", a.getLastname());
            map.put("athWeight", a.getAthWeight());
            map.put("athHeight", a.getAthHeight());
            map.put("category", a.getCategory());
            map.put("performance", a.getPerformance());
            String relStatus = rel.getStatus() != null ? rel.getStatus().getStatusName() : "pending";
            map.put("relation_status", relStatus);
            map.put("userId", a.getUserId());
            athletesWithStatus.add(map);
        }

        response.put("athletesWithStatus", athletesWithStatus);

        return ResponseEntity.ok(response);
    }

    
    @PostMapping
    public ResponseEntity<Coach> create(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("user_id");
        String name = (String) body.get("name");

        if (userIdObj == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing user_id");

        Integer userId = (userIdObj instanceof Number) ? ((Number) userIdObj).intValue() : null;
        if (userId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user_id type");

        userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Coach coach = new Coach(name != null ? name : "Unnamed Coach", userId);
        coach.setLastname((String) body.get("lastname"));
        coach.setGender((String) body.get("gender"));
        coach.setExperienceYears((body.get("experienceYears") instanceof Number)
                ? ((Number) body.get("experienceYears")).intValue() : null);
        coach.setSpecialization((String) body.get("specialization"));
        coach.setPhone((String) body.get("phone"));
        coach.setClub((String) body.get("club"));
        coach.setCountry((String) body.get("country"));
        coach.setUpdatedAt(LocalDateTime.now());

        Coach saved = coachRepo.save(coach);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<Coach> updateCoach(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Coach coach = coachRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        if (body.containsKey("name")) coach.setName((String) body.get("name"));
        if (body.containsKey("lastname")) coach.setLastname((String) body.get("lastname"));
        if (body.containsKey("gender")) coach.setGender((String) body.get("gender"));
        if (body.containsKey("experienceYears"))
            coach.setExperienceYears((body.get("experienceYears") instanceof Number)
                    ? ((Number) body.get("experienceYears")).intValue() : null);
        if (body.containsKey("specialization")) coach.setSpecialization((String) body.get("specialization"));
        if (body.containsKey("phone")) coach.setPhone((String) body.get("phone"));
        if (body.containsKey("club")) coach.setClub((String) body.get("club"));
        if (body.containsKey("country")) coach.setCountry((String) body.get("country"));

        coach.setUpdatedAt(LocalDateTime.now());
        Coach updated = coachRepo.save(coach);
        return ResponseEntity.ok(updated);
    }

    
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Coach> getByUserId(@PathVariable Integer userId) {
        Coach coach = coachRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found for this user"));
        return ResponseEntity.ok(coach);
    }

    
    @GetMapping("/{coachId}/athletes/{status}")
    public ResponseEntity<List<Map<String, Object>>> getCoachAthletesByStatus(
            @PathVariable Integer coachId,
            @PathVariable String status) {

        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (CoachAthleteRelation rel : coach.getCoachAthleteRelations()) {
            String relStatus = rel.getStatus() != null ? rel.getStatus().getStatusName() : "pending";
            if (relStatus.equalsIgnoreCase(status)) {
                Athlete a = rel.getAthlete();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("athlete_id", a.getId());
                map.put("id", a.getId());
                map.put("name", a.getName());
                map.put("lastname", a.getLastname());
                map.put("athWeight", a.getAthWeight());
                map.put("athHeight", a.getAthHeight());
                map.put("category", a.getCategory());
                map.put("performance", a.getPerformance());
                map.put("relation_status", relStatus);
                map.put("userId", a.getUserId());
                result.add(map);
            }
        }

        return ResponseEntity.ok(result);
    }

   
    @PostMapping("/{coachId}/athletes/{athleteId}")
    public ResponseEntity<String> linkAthlete(@PathVariable Integer coachId, @PathVariable Integer athleteId) {
        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        if (relationRepo.findByCoachAndAthlete(coach, athlete).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Relation already exists");
        }

        Status pending = statusRepo.findByStatusName("pending")
                .orElseGet(() -> statusRepo.save(new Status("pending")));

        relationRepo.save(new CoachAthleteRelation(coach, athlete, pending));
        return ResponseEntity.status(HttpStatus.CREATED).body("Athlete linked with status 'pending'");
    }

    
    @PutMapping("/{coachId}/athletes/{athleteId}/status/{statusName}")
    public ResponseEntity<String> updateRelationStatus(@PathVariable Integer coachId,
                                                       @PathVariable Integer athleteId,
                                                       @PathVariable String statusName) {
        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        CoachAthleteRelation relation = relationRepo.findByCoachAndAthlete(coach, athlete)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relation not found"));

        Status status = statusRepo.findByStatusName(statusName)
                .orElseGet(() -> statusRepo.save(new Status(statusName)));

        relation.setStatus(status);
        relationRepo.save(relation);

        return ResponseEntity.ok("Relation status updated to '" + statusName + "'");
    }

    
    @DeleteMapping("/{coachId}/athletes/{athleteId}")
    public ResponseEntity<String> unlinkAthlete(
            @PathVariable Integer coachId,
            @PathVariable Integer athleteId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");

        
        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        
        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        
        if (!coach.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete athletes for another coach.");
        }

        
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        
        relationRepo.findByCoachAndAthlete(coach, athlete)
                .ifPresent(relationRepo::delete);

        return ResponseEntity.ok("Athlete unlinked successfully");
    }


    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCoach(@PathVariable Integer id) {
        if (!coachRepo.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found");

        coachRepo.deleteById(id);
        return ResponseEntity.ok("Coach deleted successfully");
    }
    @GetMapping("/me")
    public ResponseEntity<?> getLoggedCoach(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        
        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        
        Coach coach = coachRepo.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No coach found for this account"));

        return ResponseEntity.ok(coach);
    }

}
