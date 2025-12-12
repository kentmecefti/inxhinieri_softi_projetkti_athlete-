package com.example.athleteresults.repositories;

import com.example.athleteresults.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    List<Role> findByUserId(Integer userId);


}
