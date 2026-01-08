package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.Result;
import com.example.athleteresults.repositories.ResultRepository;
import com.example.athleteresults.repositories.AthleteRepository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "*")
public class ResultController {

    private final ResultRepository repo;
    private final AthleteRepository athleteRepo;

    public ResultController(ResultRepository repo, AthleteRepository athleteRepo) {
        this.repo = repo;
        this.athleteRepo = athleteRepo;
    }

    
    @GetMapping
    public List<Result> all() {
        return repo.findAll();
    }

    
    @GetMapping("/athlete/{athleteId}")
    public List<Result> byAthlete(@PathVariable Integer athleteId) {
        return repo.findByAthleteId(athleteId);
    }

    
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    
    @PostMapping
    public Result create(@RequestBody Result result) {
        if (!athleteRepo.existsById(result.getAthleteId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid athleteId");
        }
        
        return repo.save(result);
    }

    
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody Result updated) {
        if (!athleteRepo.existsById(updated.getAthleteId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid athleteId");
        }

        return repo.findById(id)
                .map(r -> {
                    r.setAthleteId(updated.getAthleteId());
                    r.setRace(updated.getRace());
                    r.setRaceType(updated.getRaceType());
                    r.setRaceDate(updated.getRaceDate());
                    r.setDistance(updated.getDistance());
                    r.setTimeMs(updated.getTimeMs());
                    r.setWeight(updated.getWeight());
                    r.setNotes(updated.getNotes()); 
                    return repo.save(r);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
        }
        repo.deleteById(id);
        return "Deleted";
    }

    
    @GetMapping("/filter")
    public List<Result> filterResults(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String race,
            @RequestParam(required = false) String raceType,
            @RequestParam(required = false) Integer distance,
            @RequestParam(required = false) Integer weight,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Sort sort = Sort.by("raceDate").ascending();
        return repo.findAll(buildSpec(athleteId, race, raceType, distance, weight, from, to), sort);
    }

    
    @GetMapping("/search")
    public List<Result> searchResults(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String race,
            @RequestParam(required = false) String raceType,
            @RequestParam(required = false) Integer distance,
            @RequestParam(required = false) Integer weight,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder
    ) {
        Sort sort;

        switch (sortBy.toLowerCase()) {
            case "time":
                sort = Sort.by("timeMs");
                break;
            case "date":
            default:
                sort = Sort.by("raceDate");
                break;
        }

        sort = sortOrder.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();

        return repo.findAll(buildSpec(athleteId, race, raceType, distance, weight, fromDate, toDate), sort);
    }

    
    private Specification<Result> buildSpec(
            Integer athleteId, String race, String raceType,
            Integer distance, Integer weight, LocalDate from, LocalDate to
    ) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();
            if (athleteId != null) p = cb.and(p, cb.equal(root.get("athleteId"), athleteId));
            if (race != null && !race.isEmpty())
                p = cb.and(p, cb.like(cb.lower(root.get("race")), "%" + race.toLowerCase() + "%"));
            if (raceType != null && !raceType.isEmpty())
                p = cb.and(p, cb.like(cb.lower(root.get("raceType")), "%" + raceType.toLowerCase() + "%"));
            if (distance != null) p = cb.and(p, cb.equal(root.get("distance"), distance));
            if (weight != null) p = cb.and(p, cb.equal(root.get("weight"), weight));
            if (from != null)
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("raceDate"), from));
            if (to != null)
                p = cb.and(p, cb.lessThan(root.get("raceDate"), to.plusDays(1)));

            return p;
        };
    }
}
