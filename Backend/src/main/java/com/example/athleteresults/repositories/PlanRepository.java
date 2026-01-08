package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Plan;
import com.example.athleteresults.entities.Result;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer>, JpaSpecificationExecutor<Plan> {

    List<Plan> findByAthleteIdOrderByPlanDateAsc(Integer athleteId);

    List<Plan> findByCoachIdOrderByPlanDateAsc(Integer coachId);


    @Query(value = "SELECT * FROM plan WHERE plan_date = :date AND athlete_id = :athleteId", nativeQuery = true)
    List<Plan> findByDateAndAthleteId(@Param("date") LocalDate date, @Param("athleteId") Integer athleteId);
}
