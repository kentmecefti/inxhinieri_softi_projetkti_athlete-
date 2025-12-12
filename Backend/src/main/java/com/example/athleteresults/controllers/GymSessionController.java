package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Athlete;
import com.example.athleteresults.entities.GymSession;
import com.example.athleteresults.repositories.AthleteRepository;
import com.example.athleteresults.repositories.GymSessionRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/gymsessions")
@CrossOrigin(origins = "*")
public class GymSessionController {

    private final GymSessionRepository gymSessionRepository;
    private final AthleteRepository athleteRepository;

    public GymSessionController(GymSessionRepository gymSessionRepository, AthleteRepository athleteRepository) {
        this.gymSessionRepository = gymSessionRepository;
        this.athleteRepository = athleteRepository;
    }

    @GetMapping
    public List<GymSession> getAll() {
        return gymSessionRepository.findAll();
    }

    @GetMapping("/athlete/{athleteId}")
    public List<GymSession> getByAthlete(@PathVariable Integer athleteId) {
        return gymSessionRepository.findByAthleteId(athleteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GymSession create(@RequestBody GymSession session) {
        if (session.getAthlete() == null || session.getAthlete().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Athlete ID is required");
        }

        Athlete athlete = athleteRepository.findById(session.getAthlete().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found"));

        session.setAthlete(athlete);

        return gymSessionRepository.save(session);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!gymSessionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym session not found");
        }
        gymSessionRepository.deleteById(id);
    }

    @GetMapping("/filter")
    public List<GymSession> filter(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        List<GymSession> sessions = gymSessionRepository.findAll();
        return sessions.stream()
                .filter(s -> athleteId == null ||
                        (s.getAthlete() != null && s.getAthlete().getId().equals(athleteId)))
                .filter(s -> category == null || category.isEmpty() || s.getCategory().equalsIgnoreCase(category))
                .filter(s -> search == null || search.isEmpty() || s.getExerciseName().toLowerCase().contains(search.toLowerCase()))
                .filter(s -> {
                    if (from == null || from.isEmpty()) return true;
                    return !s.getSessionDate().isBefore(java.time.LocalDate.parse(from));
                })
                .filter(s -> {
                    if (to == null || to.isEmpty()) return true;
                    return !s.getSessionDate().isAfter(java.time.LocalDate.parse(to));
                })
                .toList();
    }
}
