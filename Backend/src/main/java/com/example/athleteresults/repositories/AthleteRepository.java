package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Integer> {

    // 🔍 Case-insensitive search by name
    List<Athlete> findByNameContainingIgnoreCase(String name);

    // 🔗 Find athlete by linked user_id
    Optional<Athlete> findByUserId(Integer userId);

    // 🌍 Find athlete by PUBLIC ID (safe for frontend / URLs)
    Optional<Athlete> findByPublicId(String publicId);

}
