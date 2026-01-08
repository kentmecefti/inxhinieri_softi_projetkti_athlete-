package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.GymSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GymSessionRepository extends JpaRepository<GymSession, Integer> {
    List<GymSession> findByAthleteId(Integer athleteId);
}
