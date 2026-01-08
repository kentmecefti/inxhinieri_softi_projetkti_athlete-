package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AthleteRepository athleteRepo;
    private final CoachRepository coachRepo;

    
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "DATA ANALYST", "COACH", "ATHLETE");

    public UserController(UserRepository userRepo,
                          RoleRepository roleRepo,
                          AthleteRepository athleteRepo,
                          CoachRepository coachRepo) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.athleteRepo = athleteRepo;
        this.coachRepo = coachRepo;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    
    @GetMapping
    public List<User> all() {
        return userRepo.findAll();
    }
    
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id, Authentication auth) {

        User requester = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean isAdmin = requester.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ADMIN"));

        if (!isAdmin && !requester.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        requester.setPassword(null);
        return ResponseEntity.ok(requester);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public User addUser(@RequestBody User user) {
        if (userRepo.existsByUsername(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        
        User savedUser = userRepo.save(user);

        
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role defaultRole = new Role("ATHLETE", savedUser);
            roleRepo.save(defaultRole);

            Athlete athlete = new Athlete();
            athlete.setName(savedUser.getUsername());
            athlete.setUserId(savedUser.getId()); 
            athleteRepo.save(athlete);
        } else {
            for (Role r : user.getRoles()) {
                String roleName = r.getName().toUpperCase();
                if (!VALID_ROLES.contains(roleName)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
                }
                roleRepo.save(new Role(roleName, savedUser));

                if (roleName.equals("ATHLETE")) {
                    Athlete athlete = new Athlete();
                    athlete.setName(savedUser.getUsername());
                    athlete.setUserId(savedUser.getId());
                    athleteRepo.save(athlete);
                }
                if (roleName.equals("COACH")) {
                    Coach coach = new Coach();
                    coach.setName(savedUser.getUsername());
                    coach.setUserId(savedUser.getId());
                    coachRepo.save(coach);
                }
            }
        }

        return savedUser;
    }

    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public User updateUser(@PathVariable Integer id, @RequestBody User updatedUser) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        existing.setUsername(updatedUser.getUsername());
        existing.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null) existing.setPassword(updatedUser.getPassword());

        
        if (updatedUser.getRoles() != null && !updatedUser.getRoles().isEmpty()) {
            roleRepo.deleteAll(roleRepo.findByUserId(id));

            for (Role r : updatedUser.getRoles()) {
                String roleName = r.getName().toUpperCase();

                if (!VALID_ROLES.contains(roleName)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
                }

                roleRepo.save(new Role(roleName, existing));
            }
        }

        return userRepo.save(existing);
    }

    
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Integer id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isCoach = user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("COACH"));
        boolean isAthlete = user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ATHLETE"));

        
        if (isCoach) coachRepo.findByUserId(id).ifPresent(coach -> coachRepo.deleteById(coach.getId()));
        if (isAthlete) athleteRepo.findByUserId(id).ifPresent(athlete -> athleteRepo.deleteById(athlete.getId()));

        
        roleRepo.deleteAll(roleRepo.findByUserId(id));
        userRepo.deleteById(id);

        return " User and linked data deleted successfully.";
    }

    
    @GetMapping("/by-username/{username}")
    public ResponseEntity<User> getByUsername(@PathVariable String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> activateUser(@PathVariable Integer id) {
        return toggleActive(id, true);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deactivateUser(@PathVariable Integer id) {
        return toggleActive(id, false);
    }

    private ResponseEntity<String> toggleActive(Integer id, boolean active) {
        return userRepo.findById(id)
                .map(user -> {
                    user.setActive(active);
                    userRepo.save(user);
                    String msg = " User " + (active ? "activated" : "deactivated") + " successfully!";
                    return ResponseEntity.ok(msg);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(" User not found with id " + id));
    }

}
