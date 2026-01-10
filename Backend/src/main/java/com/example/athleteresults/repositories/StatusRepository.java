package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {

    // Find status by its name (e.g. "pending", "accept", "refuse")
    Optional<Status> findByStatusName(String statusName);
}
