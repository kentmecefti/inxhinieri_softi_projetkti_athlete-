package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.WeightMetric;
import com.example.athleteresults.repositories.WeightMetricRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/metrics/weight")
@CrossOrigin(origins = "*")
public class WeightMetricController {

    private final WeightMetricRepository weightMetricRepository;

    public WeightMetricController(WeightMetricRepository weightMetricRepository) {
        this.weightMetricRepository = weightMetricRepository;
    }

    
    @GetMapping
    public List<WeightMetric> getByGym(@RequestParam Integer gymId) {
        return weightMetricRepository.findByGymSessionId(gymId);
    }

    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeightMetric create(@RequestBody WeightMetric metric) {
        return weightMetricRepository.save(metric);
    }

    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!weightMetricRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Weight metric not found");
        }
        weightMetricRepository.deleteById(id);
    }
}
