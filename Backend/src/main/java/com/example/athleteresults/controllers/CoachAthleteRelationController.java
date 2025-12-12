package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/relations")
@CrossOrigin(origins = "*")
public class CoachAthleteRelationController {

    private final CoachRepository coachRepo;
    private final AthleteRepository athleteRepo;
    private final CoachAthleteRelationRepository relationRepo;
    private final StatusRepository statusRepo;
    private final UserRepository userRepo;

    public CoachAthleteRelationController(CoachRepository coachRepo,
                                          AthleteRepository athleteRepo,
                                          CoachAthleteRelationRepository relationRepo,
                                          StatusRepository statusRepo,
                                          UserRepository userRepo) {
        this.coachRepo = coachRepo;
        this.athleteRepo = athleteRepo;
        this.relationRepo = relationRepo;
        this.statusRepo = statusRepo;
        this.userRepo = userRepo;
    }

    
    @PostMapping("/request")
    public ResponseEntity<String> sendRequest(
            @RequestParam Integer coachId,
            @RequestParam Integer athleteId,
            @AuthenticationPrincipal UserDetails userDetails) {

        
        if (userDetails == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");

        
        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        
        if (!coach.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Backend refused: this coach ID doesn't match your login.");
        }

       
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        
        if (relationRepo.findByCoachAndAthlete(coach, athlete).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Relation already exists or pending");
        }

        
        Status pending = statusRepo.findByStatusName("pending")
                .orElseGet(() -> statusRepo.save(new Status("pending")));

        
        CoachAthleteRelation rel = new CoachAthleteRelation(coach, athlete, pending);
        relationRepo.save(rel);

        return ResponseEntity.ok("Link request sent successfully!");
    }

    
    @PostMapping("/accept")
    public ResponseEntity<String> acceptRequest(
            @RequestParam Integer athleteId,
            @RequestParam Integer coachId) {

        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        CoachAthleteRelation relation = relationRepo.findByCoachAndAthlete(coach, athlete)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        Status accept = statusRepo.findByStatusName("accept")
                .orElseGet(() -> statusRepo.save(new Status("accept")));
        relation.setStatus(accept);
        relationRepo.save(relation);

        return ResponseEntity.ok("Request accepted successfully!");
    }

    
    @PostMapping("/refuse")
    public ResponseEntity<String> refuseRequest(
            @RequestParam Integer athleteId,
            @RequestParam Integer coachId) {

        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        CoachAthleteRelation relation = relationRepo.findByCoachAndAthlete(coach, athlete)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        Status refuse = statusRepo.findByStatusName("refuse")
                .orElseGet(() -> statusRepo.save(new Status("refuse")));
        relation.setStatus(refuse);
        relationRepo.save(relation);

        return ResponseEntity.ok("Request refused.");
    }
    @DeleteMapping("/unlink")
    public ResponseEntity<String> unlinkCoachFromAthlete(
            @RequestParam Integer coachId,
            @RequestParam Integer athleteId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");

        
        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        if (!athlete.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot unlink another athlete's coach.");
        }

        Coach coach = coachRepo.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        relationRepo.findByCoachAndAthlete(coach, athlete)
                .ifPresent(relationRepo::delete);

        return ResponseEntity.ok("Coach removed successfully");
    }

}
