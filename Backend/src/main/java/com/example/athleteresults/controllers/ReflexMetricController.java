package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.ReflexMetric;
import com.example.athleteresults.repositories.ReflexMetricRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/metrics/reflex")
@CrossOrigin(origins = "*")
public class ReflexMetricController {

    private final ReflexMetricRepository reflexMetricRepository;

    public ReflexMetricController(ReflexMetricRepository reflexMetricRepository) {
        this.reflexMetricRepository = reflexMetricRepository;
    }

    // ✅ GET all reflex metrics for a given gym session
    @GetMapping
    public List<ReflexMetric> getByGym(@RequestParam Integer gymId) {
        return reflexMetricRepository.findByGymSessionId(gymId);
    }

    // ✅ POST create new reflex metric
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReflexMetric create(@RequestBody ReflexMetric metric) {
        return reflexMetricRepository.save(metric);
    }

    // ✅ DELETE a reflex metric by ID
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!reflexMetricRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reflex metric not found");
        }
        reflexMetricRepository.deleteById(id);
    }
}
