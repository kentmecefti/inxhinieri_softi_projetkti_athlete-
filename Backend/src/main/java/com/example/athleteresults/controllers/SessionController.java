package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Session;
import com.example.athleteresults.repositories.SessionRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionRepository repo;

    public SessionController(SessionRepository repo) {
        this.repo = repo;
    }

    
    @GetMapping
    public List<Session> all() {
        return repo.findAll();
    }

    
    @GetMapping("/athlete/{athleteId}")
    public List<Session> byAthlete(
            @PathVariable Integer athleteId,
            @RequestParam(required = false) String date
    ) {
        if (date != null && !date.isEmpty()) {
            LocalDate parsedDate = LocalDate.parse(date);
            return repo.findByAthleteIdAndRunDate(athleteId, parsedDate);
        }
        return repo.findByAthleteIdOrderByRunDateDesc(athleteId);
    }

    
    @GetMapping("/{id}")
    public Session get(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Session create(@RequestBody Session session) {
        if (session.getAthleteId() == null || session.getRunDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing athlete_id or run_date");
        }
        return repo.save(session);
    }

    
    @PutMapping("/{id}")
    public Session update(@PathVariable Integer id, @RequestBody Session updated) {
        Optional<Session> existing = repo.findById(id);
        if (existing.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");

        Session s = existing.get();
        s.setRunDate(updated.getRunDate());
        s.setTimeMin(updated.getTimeMin());
        s.setDistanceKm(updated.getDistanceKm());
        s.setHeartAvg(updated.getHeartAvg());
        s.setHeartMax(updated.getHeartMax());
        s.setCalories(updated.getCalories());
        s.setSurface(updated.getSurface());
        s.setWeather(updated.getWeather());
        s.setNotes(updated.getNotes());
        return repo.save(s);
    }

    
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        repo.deleteById(id);
        return "Session deleted successfully";
    }
}
