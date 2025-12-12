package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.PlyoMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlyoMetricRepository extends JpaRepository<PlyoMetric, Integer> {
    List<PlyoMetric> findByGymSessionId(Integer gymId);
}

