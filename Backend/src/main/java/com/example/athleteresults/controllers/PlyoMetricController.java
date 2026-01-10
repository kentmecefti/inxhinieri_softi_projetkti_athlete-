package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.PlyoMetric;
import com.example.athleteresults.repositories.PlyoMetricRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/metrics/plyo")
@CrossOrigin(origins = "*")
public class PlyoMetricController {

    private final PlyoMetricRepository plyoMetricRepository;

    public PlyoMetricController(PlyoMetricRepository plyoMetricRepository) {
        this.plyoMetricRepository = plyoMetricRepository;
    }

    //  GET all plyometric metrics for a given gym session
    @GetMapping
    public List<PlyoMetric> getByGym(@RequestParam Integer gymId) {
        return plyoMetricRepository.findByGymSessionId(gymId);
    }

    //  POST create new plyometric metric
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlyoMetric create(@RequestBody PlyoMetric metric) {
        return plyoMetricRepository.save(metric);
    }

    //  DELETE a plyometric metric by ID
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!plyoMetricRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plyo metric not found");
        }
        plyoMetricRepository.deleteById(id);
    }
}
