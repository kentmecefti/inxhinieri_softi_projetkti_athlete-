package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    //  Get all sessions by athlete
    List<Session> findByAthleteId(Integer athleteId);

    //  Get all sessions by athlete, ordered by date (newest first)
    List<Session> findByAthleteIdOrderByRunDateDesc(Integer athleteId);

    //  Get sessions by athlete + specific date (for calendar filter)
    List<Session> findByAthleteIdAndRunDate(Integer athleteId, LocalDate runDate);
}

