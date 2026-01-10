package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import com.example.athleteresults.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.athleteresults.dto.ChangePasswordRequest;


import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AthleteRepository athleteRepo;
    private final CoachRepository coachRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    // Allowed role names
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "DATA ANALYST", "COACH", "ATHLETE");

    public AuthController(AuthenticationManager authManager,
                          UserRepository userRepo,
                          RoleRepository roleRepo,
                          AthleteRepository athleteRepo,
                          CoachRepository coachRepo,
                          PasswordEncoder encoder,
                          JwtService jwtService) {
        this.authManager = authManager;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.athleteRepo = athleteRepo;
        this.coachRepo = coachRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    // ========================= REGISTER =========================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        // Validate and normalize role
        String roleName = (req.getRole() != null) ? req.getRole().trim().toUpperCase() : "ATHLETE";
        if (!VALID_ROLES.contains(roleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
        }

        // 1️⃣ Create user
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        User savedUser = userRepo.save(user);

        // 2️⃣ Assign role
        Role role = new Role(roleName, savedUser);
        roleRepo.save(role);

        // 3️⃣ Auto-link based on role
        switch (roleName) {
            case "ATHLETE" -> {
                Athlete athlete = new Athlete();
                athlete.setName(savedUser.getUsername());
                athlete.setUser(savedUser);
                athleteRepo.save(athlete);
            }
            case "COACH" -> {
                Coach coach = new Coach();
                coach.setName(savedUser.getUsername());
                coach.setUser(savedUser);
                coachRepo.save(coach);
            }
            default -> {
                // For ADMIN or DATA ANALYST, no link to athlete/coach tables
            }
        }

        return ResponseEntity.ok("✅ User registered successfully as " + roleName);
    }

    // ========================= LOGIN =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            // Try to authenticate first
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );

            // Retrieve full user entity
            User foundUser = userRepo.findByUsername(user.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user is active
            if (!foundUser.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(" User account is inactive. Please contact an administrator.");
            }

            // Generate JWT token
            String token = jwtService.generateToken(user.getUsername());
            return ResponseEntity.ok(token);

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(" Invalid username or password.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(" Login failed: " + ex.getMessage());
        }
    }


    // ========================= DTO CLASS =========================
    public static class RegistrationRequest {
        private String username;
        private String email;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
    // ========================= CHANGE PASSWORD =========================
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Unauthorized");
        }

        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        // Check current password
        if (!encoder.matches(req.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Current password is incorrect");
        }

        // Update password
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.saveAndFlush(user);

        return ResponseEntity.ok("✅ Password changed successfully");
    }
}
