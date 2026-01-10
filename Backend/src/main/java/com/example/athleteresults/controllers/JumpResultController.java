package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.JumpResult;
import com.example.athleteresults.repositories.AthleteRepository;
import com.example.athleteresults.repositories.JumpResultRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/jumpresults")
@CrossOrigin(origins = "*")
public class JumpResultController {

    private final JumpResultRepository repo;
    private final AthleteRepository athleteRepo;

    public JumpResultController(JumpResultRepository repo, AthleteRepository athleteRepo) {
        this.repo = repo;
        this.athleteRepo = athleteRepo;
    }

    /* =====================================================
       GET ALL
    ===================================================== */
    @GetMapping
    public List<JumpResult> all() {
        return repo.findAll();
    }

    /* =====================================================
       GET BY ATHLETE
    ===================================================== */
    @GetMapping("/athlete/{athleteId}")
    public List<JumpResult> byAthlete(@PathVariable Integer athleteId) {
        return repo.findByAthleteId(athleteId);
    }

    /* =====================================================
       GET BY ID
    ===================================================== */
    @GetMapping("/{id}")
    public JumpResult byId(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Jump result not found with ID: " + id
                ));
    }

    /* =====================================================
       CREATE
    ===================================================== */
    @PostMapping
    public JumpResult create(@RequestBody JumpResult newJump) {
        if (!athleteRepo.existsById(newJump.getAthleteId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Athlete not found with ID: " + newJump.getAthleteId()
            );
        }
        return repo.save(newJump);
    }

    /* =====================================================
       UPDATE
    ===================================================== */
    @PutMapping("/{id}")
    public JumpResult update(@PathVariable Integer id, @RequestBody JumpResult data) {
        return repo.findById(id).map(jump -> {
            jump.setJumpDate(data.getJumpDate());
            jump.setJumpType(data.getJumpType());
            jump.setDetail(data.getDetail());
            jump.setDistanceM(data.getDistanceM());
            jump.setAthleteId(data.getAthleteId());
            jump.setNotes(data.getNotes());
            return repo.save(jump);
        }).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Jump result not found with ID: " + id
        ));
    }

    /* =====================================================
       DELETE
    ===================================================== */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Jump result not found with ID: " + id
            );
        }
        repo.deleteById(id);
        return "iku";
    }

    /* =====================================================
       FILTER
    ===================================================== */
    @GetMapping("/filter")
    public List<JumpResult> filterJumps(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String jumpType,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return repo.findAll(buildSpec(athleteId, jumpType, detail, from, to),
                Sort.by("jumpDate").ascending());
    }

    /* =====================================================
       SEARCH
    ===================================================== */
    @GetMapping("/search")
    public List<JumpResult> searchJumps(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String jumpType,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder
    ) {
        Sort sort = sortBy.equalsIgnoreCase("distancem")
                ? Sort.by("distanceM")
                : Sort.by("jumpDate");

        sort = sortOrder.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();

        return repo.findAll(buildSpec(athleteId, jumpType, detail, from, to), sort);
    }

    /* =====================================================
       SPECIFICATION
    ===================================================== */
    private Specification<JumpResult> buildSpec(
            Integer athleteId, String jumpType, String detail,
            LocalDate from, LocalDate to
    ) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (athleteId != null)
                p = cb.and(p, cb.equal(root.get("athleteId"), athleteId));

            if (jumpType != null && !jumpType.isEmpty())
                p = cb.and(p,
                        cb.like(cb.lower(root.get("jumpType")),
                                "%" + jumpType.toLowerCase() + "%"));

            if (detail != null && !detail.isEmpty())
                p = cb.and(p,
                        cb.like(cb.lower(root.get("detail")),
                                "%" + detail.toLowerCase() + "%"));

            if (from != null)
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("jumpDate"), from));

            if (to != null)
                p = cb.and(p, cb.lessThan(root.get("jumpDate"), to.plusDays(1)));

            return p;
        };
    }
}
