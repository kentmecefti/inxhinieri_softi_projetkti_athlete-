package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.JumpResult;
import com.example.athleteresults.repositories.JumpResultRepository;
import com.example.athleteresults.repositories.AthleteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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

    
    @GetMapping
    public List<JumpResult> all() {
        return repo.findAll();
    }

    
    @GetMapping("/athlete/{athleteId}")
    public List<JumpResult> byAthlete(@PathVariable Integer athleteId) {
        return repo.findByAthleteId(athleteId);
    }

    
    @GetMapping("/{id}")
    public JumpResult byId(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Jump result not found with ID: " + id));
    }

    
    @PostMapping
    public JumpResult create(@RequestBody JumpResult newJump) {
        if (!athleteRepo.existsById(newJump.getAthleteId())) {
            throw new RuntimeException("Athlete not found with ID: " + newJump.getAthleteId());
        }
        return repo.save(newJump);
    }

    
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
        }).orElseThrow(() -> new RuntimeException("Jump result not found with ID: " + id));
    }

    
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "iku";
    }

    
    @GetMapping("/filter")
    public List<JumpResult> filterJumps(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String jumpType,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Sort sort = Sort.by("jumpDate").ascending();
        return repo.findAll(buildSpec(athleteId, jumpType, detail, from, to), sort);
    }

    
    @GetMapping("/search")
    public List<JumpResult> searchJumps(
            @RequestParam(required = false) Integer athleteId,
            @RequestParam(required = false) String jumpType,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder
    ) {
        Sort sort;
        switch (sortBy.toLowerCase()) {
            case "distancem":
                sort = Sort.by("distanceM");
                break;
            case "date":
            default:
                sort = Sort.by("jumpDate");
                break;
        }

        sort = sortOrder.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();

        return repo.findAll(buildSpec(athleteId, jumpType, detail, from, to), sort);
    }

    
    private Specification<JumpResult> buildSpec(
            Integer athleteId, String jumpType, String detail, LocalDate from, LocalDate to
    ) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (athleteId != null)
                p = cb.and(p, cb.equal(root.get("athleteId"), athleteId));

            if (jumpType != null && !jumpType.isEmpty())
                p = cb.and(p, cb.like(cb.lower(root.get("jumpType")), "%" + jumpType.toLowerCase() + "%"));

            if (detail != null && !detail.isEmpty())
                p = cb.and(p, cb.like(cb.lower(root.get("detail")), "%" + detail.toLowerCase() + "%"));

            if (from != null)
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("jumpDate"), from));

            if (to != null)
                p = cb.and(p, cb.lessThan(root.get("jumpDate"), to.plusDays(1)));

            return p;
        };
    }
}
