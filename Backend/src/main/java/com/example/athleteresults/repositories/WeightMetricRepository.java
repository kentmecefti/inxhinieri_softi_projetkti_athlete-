
package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.WeightMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WeightMetricRepository extends JpaRepository<WeightMetric, Integer> {
    List<WeightMetric> findByGymSessionId(Integer gymId);
}
