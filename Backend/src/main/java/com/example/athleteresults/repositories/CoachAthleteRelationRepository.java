package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoachAthleteRelationRepository extends JpaRepository<CoachAthleteRelation, Long> {

    // Check if a relation exists between coach and athlete
    Optional<CoachAthleteRelation> findByCoachAndAthlete(Coach coach, Athlete athlete);

    boolean existsByCoachIdAndAthleteIdAndStatusId(Integer coachId, Integer athleteId, Integer statusId);

}
