package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.ThrowResult;
import com.example.athleteresults.repositories.ThrowResultRepository;
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
@RequestMapping("/api/throwresults")
@CrossOrigin(origins = "*")
public class ThrowResultController {

    private final ThrowResultRepository repo;
    private final AthleteRepository athleteRepo;

    public ThrowResultController(ThrowResultRepository repo, AthleteRepository athleteRepo) {
        this.repo = repo;
        this.athleteRepo = athleteRepo;
    }

    // ===== GET — all results =====
    @GetMapping
    public List<ThrowResult> all() {
        return repo.findAll();
    }

    // ===== GET — by athlete =====
    @GetMapping("/athlete/{athleteId}")
    public List<ThrowResult> byAthlete(@PathVariable Integer athleteId) {
        return repo.findByAthleteId(athleteId);
    }

    // ===== GET — single result =====
    @GetMapping("/{id}")
    public ThrowResult getById(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Throw result not found"));
    }

    // ===== POST — create new =====
    @PostMapping
    public ThrowResult create(@RequestBody ThrowResult result) {
        if (result.getAthlete() == null || result.getAthlete().getId() == null ||
                !athleteRepo.existsById(result.getAthlete().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid athleteId");
        }
        return repo.save(result);
    }

    // ===== PUT — update existing =====
    @PutMapping("/{id}")
    public ThrowResult update(@PathVariable Integer id, @RequestBody ThrowResult updated) {
        if (updated.getAthlete() == null || updated.getAthlete().getId() == null ||
                !athleteRepo.existsById(updated.getAthlete().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid athleteId");
        }

        return repo.findById(id)
                .map(r -> {
                    r.setAthlete(updated.getAthlete());
                    r.setThrowDate(updated.getThrowDate());
                    r.setThrowType(updated.getThrowType());
                    r.setEvent(updated.getEvent());
                    r.setDistance(updated.getDistance());
                    r.setWind(updated.getWind());
                    r.setNotes(updated.getNotes());
                    r.setThrowStyle(updated.getThrowStyle());
                    return repo.save(r);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Throw result not found"));
    }

    // ===== DELETE — by ID =====
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Throw result not found");
        }
        repo.deleteById(id);
        return "tralalala";
    }

    // ===== FILTER — date range / type / event / style =====
    @GetMapping("/filter")
    public List<ThrowResult> filterResults(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String throwType,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String throwStyle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Sort sort = Sort.by("throwDate").ascending();
        return repo.findAll(buildSpec(athleteId, throwType, event, throwStyle, from, to), sort);
    }

    // ===== SEARCH — supports sorting by distance/date & filters =====
    @GetMapping("/search")
    public List<ThrowResult> searchResults(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String throwType,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String throwStyle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        // ===== Determine Sort Field =====
        String sortField;
        switch (sortBy.toLowerCase()) {
            case "distance":
                sortField = "distance";
                break;
            case "date":
            default:
                sortField = "throwDate";
                break;
        }

        // ===== Determine Sort Direction =====
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(Sort.Direction.ASC, sortField)
                : Sort.by(Sort.Direction.DESC, sortField);

        // ===== Execute Query =====
        return repo.findAll(buildSpec(athleteId, throwType, event, throwStyle, from, to), sort);
    }

    // ===== Helper — dynamic filter builder =====
    private Specification<ThrowResult> buildSpec(
            Integer athleteId,
            String throwType,
            String event,
            String throwStyle,
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (athleteId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("athlete").get("id"), athleteId));
            }
            if (throwType != null && !throwType.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("throwType")), "%" + throwType.toLowerCase() + "%"));
            }
            if (event != null && !event.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("event")), "%" + event.toLowerCase() + "%"));
            }
            if (throwStyle != null && !throwStyle.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("throwStyle")), "%" + throwStyle.toLowerCase() + "%"));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("throwDate"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("throwDate"), to));
            }

            return predicate;
        };
    }
}
