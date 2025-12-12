package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.ThrowResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface ThrowResultRepository
        extends JpaRepository<ThrowResult, Integer>, JpaSpecificationExecutor<ThrowResult> {
    List<ThrowResult> findByAthleteId(Integer athleteId);
}

