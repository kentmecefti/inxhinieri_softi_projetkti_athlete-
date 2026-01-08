
package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.ReflexMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReflexMetricRepository extends JpaRepository<ReflexMetric, Integer> {
    // Returns all ReflexMetric records for a given gym session ID
    List<ReflexMetric> findByGymSessionId(Integer gymId);
}
