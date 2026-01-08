package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Integer>, JpaSpecificationExecutor<Result> {
    List<Result> findByAthleteId(Integer athleteId);
    void deleteByAthleteId(Integer athleteId);
}
