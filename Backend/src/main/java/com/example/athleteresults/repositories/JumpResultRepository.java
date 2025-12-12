package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.JumpResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface JumpResultRepository extends JpaRepository<JumpResult, Integer>, JpaSpecificationExecutor<JumpResult> {
    List<JumpResult> findByAthleteId(Integer athleteId);
    void deleteByAthleteId(Integer athleteId);
}

