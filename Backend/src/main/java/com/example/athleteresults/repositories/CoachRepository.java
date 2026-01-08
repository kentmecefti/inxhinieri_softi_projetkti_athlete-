package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Integer> {
    Optional<Coach> findByUserId(Integer userId);

}
